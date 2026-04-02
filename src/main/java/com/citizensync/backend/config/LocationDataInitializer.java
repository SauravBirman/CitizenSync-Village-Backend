package com.citizensync.backend.config;

import com.citizensync.backend.entity.*;
import com.citizensync.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationDataInitializer implements CommandLineRunner {

    private static final String DISTRICT_FILE_NAME = "All_Districtof_India_2026-03-25_15-25-21.xlsx";
    private static final String VILLAGE_FILE_NAME = "Village_Gram_Panchayat_Mapping_2026-03-25_15-16-06.xlsx";
    private static final String VILLAGE_CSV_FILE_NAME = "Village_Gram_Panchayat_Mapping_2026-03-25_15-16-06.csv";
    private static final String RAJASTHAN_ONLY_CSV_FILE_NAME = "Rajasthan_District_Subdistrict_LocalBody_Village.csv";

    private final LocationStateRepository stateRepository;
    private final LocationDistrictRepository districtRepository;
    private final LocationSubdistrictRepository subdistrictRepository;
    private final LocationPanchayatRepository panchayatRepository;
    private final LocationVillageRepository villageRepository;

    @Value("${app.location.reload-data:false}")
    private boolean reloadLocationData;

    private final DataFormatter dataFormatter = new DataFormatter();

    static {
        // Allow loading large XLSX shared strings / worksheet entries from official govt datasets.
        IOUtils.setByteArrayMaxOverride(600_000_000);
        ZipSecureFile.setMaxEntrySize(600_000_000L);
    }

    @Override
    public void run(String... args) {
        long existingStateCount = stateRepository.count();
        if (!reloadLocationData && existingStateCount > 0) {
            log.info("Location import skipped. Existing location data found (states={}). Set app.location.reload-data=true to force reload.",
                    existingStateCount);
            return;
        }

        if (reloadLocationData) {
            log.info("Location reload is enabled via app.location.reload-data=true.");
        }

        Path rajasthanOnlyCsvPath = resolveImportFile(RAJASTHAN_ONLY_CSV_FILE_NAME);
        Path districtPath = resolveImportFile(DISTRICT_FILE_NAME);
        Path villagePath = resolveImportFile(VILLAGE_FILE_NAME);

        if (rajasthanOnlyCsvPath == null && districtPath == null && villagePath == null) {
            log.warn("Location import skipped. Could not find '{}', '{}' or '{}' near project root.",
                    RAJASTHAN_ONLY_CSV_FILE_NAME, DISTRICT_FILE_NAME, VILLAGE_FILE_NAME);
            return;
        }

        Map<String, LocationState> stateByName = new HashMap<>();
        Map<String, LocationDistrict> districtByParentAndName = new HashMap<>();
        Map<String, LocationSubdistrict> subdistrictByParentAndName = new HashMap<>();
        Map<String, LocationPanchayat> panchayatByParentAndName = new HashMap<>();
        Map<String, LocationVillage> villageByParentAndName = new HashMap<>();
        Set<String> usedStateCodes = new HashSet<>();

        preloadExisting(stateByName, districtByParentAndName, subdistrictByParentAndName,
                panchayatByParentAndName, villageByParentAndName, usedStateCodes);

        if (rajasthanOnlyCsvPath != null) {
            log.info("Rajasthan-only CSV found at {}. Replacing location hierarchy with this file only.", rajasthanOnlyCsvPath);
            clearLocationHierarchy();
            stateByName.clear();
            districtByParentAndName.clear();
            subdistrictByParentAndName.clear();
            panchayatByParentAndName.clear();
            villageByParentAndName.clear();
            usedStateCodes.clear();

            try {
                importVillageCsv(rajasthanOnlyCsvPath, stateByName, districtByParentAndName, subdistrictByParentAndName,
                        panchayatByParentAndName, villageByParentAndName, usedStateCodes);
            } catch (Exception ex) {
                log.error("Rajasthan-only CSV import failed for {}. Continuing startup.", rajasthanOnlyCsvPath, ex);
            }

            log.info("Location import complete. states={}, districts={}, subdistricts={}, panchayats={}, villages={}",
                    stateRepository.count(),
                    districtRepository.count(),
                    subdistrictRepository.count(),
                    panchayatRepository.count(),
                    villageRepository.count());
            return;
        }

        if (districtPath != null) {
            try {
                importDistrictWorkbook(districtPath, stateByName, districtByParentAndName, usedStateCodes);
            } catch (Exception ex) {
                log.error("District workbook import failed for {}. Continuing startup.", districtPath, ex);
            }
        }

        if (villagePath != null) {
            try {
                importVillageWorkbook(villagePath, stateByName, districtByParentAndName,
                        subdistrictByParentAndName, panchayatByParentAndName, villageByParentAndName, usedStateCodes);
            } catch (Exception ex) {
                log.error("Village workbook import failed for {}. Continuing startup.", villagePath, ex);
                tryVillageCsvFallback(stateByName, districtByParentAndName, subdistrictByParentAndName,
                        panchayatByParentAndName, villageByParentAndName, usedStateCodes);
            }
        } else {
            tryVillageCsvFallback(stateByName, districtByParentAndName, subdistrictByParentAndName,
                    panchayatByParentAndName, villageByParentAndName, usedStateCodes);
        }

        log.info("Location import complete. states={}, districts={}, subdistricts={}, panchayats={}, villages={}",
                stateRepository.count(),
                districtRepository.count(),
                subdistrictRepository.count(),
                panchayatRepository.count(),
                villageRepository.count());
    }

    private void preloadExisting(
            Map<String, LocationState> stateByName,
            Map<String, LocationDistrict> districtByParentAndName,
            Map<String, LocationSubdistrict> subdistrictByParentAndName,
            Map<String, LocationPanchayat> panchayatByParentAndName,
            Map<String, LocationVillage> villageByParentAndName,
            Set<String> usedStateCodes
    ) {
        for (LocationState state : stateRepository.findAll()) {
            stateByName.put(nameKey(state.getName()), state);
            usedStateCodes.add(state.getCode());
        }

        for (LocationDistrict district : districtRepository.findAll()) {
            districtByParentAndName.put(composeKey(district.getState().getId(), district.getName()), district);
        }

        for (LocationSubdistrict subdistrict : subdistrictRepository.findAll()) {
            subdistrictByParentAndName.put(composeKey(subdistrict.getDistrict().getId(), subdistrict.getName()), subdistrict);
        }

        for (LocationPanchayat panchayat : panchayatRepository.findAll()) {
            panchayatByParentAndName.put(composeKey(panchayat.getSubdistrict().getId(), panchayat.getName()), panchayat);
        }

        for (LocationVillage village : villageRepository.findAll()) {
            villageByParentAndName.put(composeKey(village.getPanchayat().getId(), village.getName()), village);
        }
    }

    private void importDistrictWorkbook(
            Path workbookPath,
            Map<String, LocationState> stateByName,
            Map<String, LocationDistrict> districtByParentAndName,
            Set<String> usedStateCodes
    ) {
        try (InputStream inputStream = Files.newInputStream(workbookPath); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return;
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = extractHeaders(headerRow);

            int stateCol = findColumn(headers, List.of("state", "stateut", "state name", "state/ut", "state_name"));
            int districtCol = findColumn(headers, List.of("district", "district name", "district_name"));

            if (stateCol < 0 || districtCol < 0) {
                log.warn("Skipping district workbook import due to missing required columns. headers={}", headers);
                return;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String stateName = cleanCell(row, stateCol);
                String districtName = cleanCell(row, districtCol);

                if (stateName == null || districtName == null) {
                    continue;
                }

                LocationState state = getOrCreateState(stateName, stateByName, usedStateCodes);
                getOrCreateDistrict(state, districtName, districtByParentAndName);
            }

            log.info("Imported district workbook from {}", workbookPath);
        } catch (Exception ex) {
            log.error("Failed to import district workbook {}", workbookPath, ex);
        }
    }

    private void importVillageWorkbook(
            Path workbookPath,
            Map<String, LocationState> stateByName,
            Map<String, LocationDistrict> districtByParentAndName,
            Map<String, LocationSubdistrict> subdistrictByParentAndName,
            Map<String, LocationPanchayat> panchayatByParentAndName,
            Map<String, LocationVillage> villageByParentAndName,
            Set<String> usedStateCodes
    ) {
        try (InputStream inputStream = Files.newInputStream(workbookPath); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                return;
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = extractHeaders(headerRow);

            int stateCol = findColumn(headers, List.of("state", "stateut", "state name", "state/ut", "state_name"));
            int districtCol = findColumn(headers, List.of("district", "district name", "district_name"));
            int subdistrictCol = findColumn(headers, List.of("subdistrict", "sub district", "tehsil", "taluka", "block", "block name"));
            int panchayatCol = findColumn(headers, List.of("panchayat", "gram panchayat", "grampanchayat", "gp", "local body", "localbody", "local body name"));
            int villageCol = findColumn(headers, List.of("village", "village name", "villagename", "revenue village"));

            if (stateCol < 0 || districtCol < 0 || panchayatCol < 0 || villageCol < 0) {
                log.warn("Skipping village workbook import due to missing required columns. headers={}", headers);
                return;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String stateName = cleanCell(row, stateCol);
                String districtName = cleanCell(row, districtCol);
                String subdistrictName = subdistrictCol >= 0 ? cleanCell(row, subdistrictCol) : null;
                String panchayatName = cleanCell(row, panchayatCol);
                String villageName = cleanCell(row, villageCol);

                if (stateName == null || districtName == null || panchayatName == null || villageName == null) {
                    continue;
                }

                LocationState state = getOrCreateState(stateName, stateByName, usedStateCodes);
                LocationDistrict district = getOrCreateDistrict(state, districtName, districtByParentAndName);

                String safeSubdistrictName = (subdistrictName == null || subdistrictName.isBlank())
                        ? (districtName + " Block")
                        : subdistrictName;
                LocationSubdistrict subdistrict = getOrCreateSubdistrict(district, safeSubdistrictName, subdistrictByParentAndName);

                LocationPanchayat panchayat = getOrCreatePanchayat(subdistrict, panchayatName, panchayatByParentAndName);
                getOrCreateVillage(panchayat, villageName, villageByParentAndName);
            }

            log.info("Imported village workbook from {}", workbookPath);
        } catch (Exception ex) {
            log.error("Failed to import village workbook {}", workbookPath, ex);
        }
    }

    private void tryVillageCsvFallback(
            Map<String, LocationState> stateByName,
            Map<String, LocationDistrict> districtByParentAndName,
            Map<String, LocationSubdistrict> subdistrictByParentAndName,
            Map<String, LocationPanchayat> panchayatByParentAndName,
            Map<String, LocationVillage> villageByParentAndName,
            Set<String> usedStateCodes
    ) {
        Path csvPath = resolveImportFile(VILLAGE_CSV_FILE_NAME);
        if (csvPath == null) {
            log.warn("Village CSV fallback not found. Expected '{}'.", VILLAGE_CSV_FILE_NAME);
            return;
        }

        try {
            importVillageCsv(csvPath, stateByName, districtByParentAndName, subdistrictByParentAndName,
                    panchayatByParentAndName, villageByParentAndName, usedStateCodes);
        } catch (Exception ex) {
            log.error("Village CSV fallback import failed for {}", csvPath, ex);
        }
    }

    private void importVillageCsv(
            Path csvPath,
            Map<String, LocationState> stateByName,
            Map<String, LocationDistrict> districtByParentAndName,
            Map<String, LocationSubdistrict> subdistrictByParentAndName,
            Map<String, LocationPanchayat> panchayatByParentAndName,
            Map<String, LocationVillage> villageByParentAndName,
            Set<String> usedStateCodes
    ) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }

            List<String> headers = parseCsvLine(headerLine);

            int stateCol = findColumn(headers, List.of("state", "stateut", "state name", "state/ut", "state_name"));
            int districtCol = findColumn(headers, List.of("district", "district name", "district_name"));
            int subdistrictCol = findColumn(headers, List.of("subdistrict", "sub district", "tehsil", "taluka", "block", "block name"));
            int panchayatCol = findColumn(headers, List.of("panchayat", "gram panchayat", "grampanchayat", "gp", "local body", "localbody", "local body name"));
            int villageCol = findColumn(headers, List.of("village", "village name", "villagename", "revenue village"));

            log.info("CSV Location column mapping: state={} ({}), district={} ({}), subdistrict={} ({}), panchayat={} ({}), village={} ({})",
                    stateCol, stateCol >= 0 ? headers.get(stateCol) : "NOT_FOUND",
                    districtCol, districtCol >= 0 ? headers.get(districtCol) : "NOT_FOUND",
                    subdistrictCol, subdistrictCol >= 0 ? headers.get(subdistrictCol) : "NOT_FOUND",
                    panchayatCol, panchayatCol >= 0 ? headers.get(panchayatCol) : "NOT_FOUND",
                    villageCol, villageCol >= 0 ? headers.get(villageCol) : "NOT_FOUND");

            if (stateCol < 0 || districtCol < 0 || panchayatCol < 0 || villageCol < 0) {
                log.warn("Skipping village CSV import due to missing required columns. headers={}", headers);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                if (row.isEmpty()) {
                    continue;
                }

                String stateName = valueAt(row, stateCol);
                String districtName = valueAt(row, districtCol);
                String subdistrictName = subdistrictCol >= 0 ? valueAt(row, subdistrictCol) : null;
                String panchayatName = valueAt(row, panchayatCol);
                String villageName = valueAt(row, villageCol);

                if (stateName == null || districtName == null || panchayatName == null || villageName == null) {
                    continue;
                }

                LocationState state = getOrCreateState(stateName, stateByName, usedStateCodes);
                LocationDistrict district = getOrCreateDistrict(state, districtName, districtByParentAndName);

                String safeSubdistrictName = (subdistrictName == null || subdistrictName.isBlank())
                        ? (districtName + " Block")
                        : subdistrictName;
                LocationSubdistrict subdistrict = getOrCreateSubdistrict(district, safeSubdistrictName, subdistrictByParentAndName);

                LocationPanchayat panchayat = getOrCreatePanchayat(subdistrict, panchayatName, panchayatByParentAndName);
                getOrCreateVillage(panchayat, villageName, villageByParentAndName);
            }
        }

        log.info("Imported village CSV fallback from {}", csvPath);
    }

    private void clearLocationHierarchy() {
        try {
            // Delete in reverse dependency order
            villageRepository.deleteAll();
            panchayatRepository.deleteAll();
            subdistrictRepository.deleteAll();
            districtRepository.deleteAll();
            stateRepository.deleteAll();
            log.info("Successfully cleared all location hierarchy tables");
        } catch (Exception ex) {
            log.warn("Failed to clear location hierarchy. This may be OK if tables were already empty.", ex);
        }
    }

    private String generateUniqueCode(String baseCode, Long parentId, java.util.Collection<?> collection, String parentType) {
        String code = baseCode;
        int suffix = 1;
        boolean isDuplicate = true;
        
        while (isDuplicate) {
            final String codeToCheck = code;
            final Long checkParentId = parentId;
            
            isDuplicate = collection.stream().anyMatch(item -> {
                if ("state".equals(parentType) && item instanceof LocationDistrict) {
                    return ((LocationDistrict) item).getState().getId().equals(checkParentId) && 
                           ((LocationDistrict) item).getCode().equals(codeToCheck);
                } else if ("district".equals(parentType) && item instanceof LocationSubdistrict) {
                    return ((LocationSubdistrict) item).getDistrict().getId().equals(checkParentId) && 
                           ((LocationSubdistrict) item).getCode().equals(codeToCheck);
                } else if ("subdistrict".equals(parentType) && item instanceof LocationPanchayat) {
                    return ((LocationPanchayat) item).getSubdistrict().getId().equals(checkParentId) && 
                           ((LocationPanchayat) item).getCode().equals(codeToCheck);
                } else if ("panchayat".equals(parentType) && item instanceof LocationVillage) {
                    return ((LocationVillage) item).getPanchayat().getId().equals(checkParentId) && 
                           ((LocationVillage) item).getCode().equals(codeToCheck);
                }
                return false;
            });
            
            if (isDuplicate) {
                code = baseCode + "-" + suffix;
                suffix++;
            }
        }
        
        return code;
    }

    private List<String> parseCsvLine(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(cleanText(current.toString()));
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        values.add(cleanText(current.toString()));
        return values;
    }

    private String valueAt(List<String> row, int index) {
        if (index < 0 || index >= row.size()) {
            return null;
        }
        return cleanText(row.get(index));
    }

    private LocationState getOrCreateState(String name, Map<String, LocationState> stateByName, Set<String> usedStateCodes) {
        String key = nameKey(name);
        LocationState existing = stateByName.get(key);
        if (existing != null) {
            return existing;
        }

        LocationState state = new LocationState();
        state.setName(name);
        state.setCode(generateStateCode(name, usedStateCodes));
        state = stateRepository.save(state);
        stateByName.put(key, state);
        usedStateCodes.add(state.getCode());
        return state;
    }

    private LocationDistrict getOrCreateDistrict(LocationState state, String name,
                                                 Map<String, LocationDistrict> districtByParentAndName) {
        String key = composeKey(state.getId(), name);
        LocationDistrict existing = districtByParentAndName.get(key);
        if (existing != null) {
            return existing;
        }

        LocationDistrict district = new LocationDistrict();
        district.setState(state);
        district.setName(name);
        String baseCode = "D-" + shortHash(state.getId() + "-" + name);
        String finalCode = generateUniqueCode(baseCode, state.getId(), districtByParentAndName.values(), "state");
        district.setCode(finalCode);
        district = districtRepository.save(district);
        districtByParentAndName.put(key, district);
        return district;
    }

    private LocationSubdistrict getOrCreateSubdistrict(LocationDistrict district, String name,
                                                       Map<String, LocationSubdistrict> subdistrictByParentAndName) {
        String key = composeKey(district.getId(), name);
        LocationSubdistrict existing = subdistrictByParentAndName.get(key);
        if (existing != null) {
            return existing;
        }

        // Reject numeric-only names (likely codes instead of real names)
        if (name != null && name.matches("^\\d+$")) {
            log.warn("Skipping subdistrict with numeric-only name '{}' in district {}. This suggests wrong CSV column was selected.",
                    name, district.getName());
            return null;
        }

        LocationSubdistrict subdistrict = new LocationSubdistrict();
        subdistrict.setDistrict(district);
        subdistrict.setName(name);
        String baseCode = "SD-" + shortHash(district.getId() + "-" + name);
        String finalCode = generateUniqueCode(baseCode, district.getId(), subdistrictByParentAndName.values(), "district");
        subdistrict.setCode(finalCode);
        subdistrict = subdistrictRepository.save(subdistrict);
        subdistrictByParentAndName.put(key, subdistrict);
        return subdistrict;
    }

    private LocationPanchayat getOrCreatePanchayat(LocationSubdistrict subdistrict, String name,
                                                   Map<String, LocationPanchayat> panchayatByParentAndName) {
        String key = composeKey(subdistrict.getId(), name);
        LocationPanchayat existing = panchayatByParentAndName.get(key);
        if (existing != null) {
            return existing;
        }

        LocationPanchayat panchayat = new LocationPanchayat();
        panchayat.setSubdistrict(subdistrict);
        panchayat.setName(name);
        String baseCode = "P-" + shortHash(subdistrict.getId() + "-" + name);
        String finalCode = generateUniqueCode(baseCode, subdistrict.getId(), panchayatByParentAndName.values(), "subdistrict");
        panchayat.setCode(finalCode);
        panchayat = panchayatRepository.save(panchayat);
        panchayatByParentAndName.put(key, panchayat);
        return panchayat;
    }

    private LocationVillage getOrCreateVillage(LocationPanchayat panchayat, String name,
                                               Map<String, LocationVillage> villageByParentAndName) {
        String key = composeKey(panchayat.getId(), name);
        LocationVillage existing = villageByParentAndName.get(key);
        if (existing != null) {
            return existing;
        }

        LocationVillage village = new LocationVillage();
        village.setPanchayat(panchayat);
        village.setName(name);
        String baseCode = "V-" + shortHash(panchayat.getId() + "-" + name);
        String finalCode = generateUniqueCode(baseCode, panchayat.getId(), villageByParentAndName.values(), "panchayat");
        village.setCode(finalCode);
        village = villageRepository.save(village);
        villageByParentAndName.put(key, village);
        return village;
    }

    private List<String> extractHeaders(Row headerRow) {
        if (headerRow == null) {
            return List.of();
        }

        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            headers.add(cleanText(dataFormatter.formatCellValue(headerRow.getCell(i))));
        }
        return headers;
    }

    private int findColumn(List<String> headers, List<String> aliases) {
        List<String> normalizedAliases = aliases.stream().map(this::headerKey).toList();
        List<String> normalizedHeaders = headers.stream().map(this::headerKey).toList();

        // First pass: exact match (like "subdistrict" aliases to normalized header "subdistrict")
        for (int i = 0; i < normalizedHeaders.size(); i++) {
            String key = normalizedHeaders.get(i);
            if (normalizedAliases.contains(key)) {
                return i;
            }
        }

        // Second pass: prefer explicit *_Name or *_name columns over code columns
        for (int i = 0; i < normalizedHeaders.size(); i++) {
            String key = normalizedHeaders.get(i);
            for (String alias : normalizedAliases) {
                if (key.contains(alias) && key.contains("name") && !key.contains("code")) {
                    return i;
                }
            }
        }

        // Third pass: fuzzy match with code-filter (skip code columns when a name alias is requested)
        for (int i = 0; i < normalizedHeaders.size(); i++) {
            String key = normalizedHeaders.get(i);
            for (String alias : normalizedAliases) {
                if ((key.contains(alias) || alias.contains(key)) && !isCodeLikeColumnMatch(key, alias)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isCodeLikeColumnMatch(String headerKey, String aliasKey) {
        boolean headerLooksCodeLike = headerKey.contains("code") || headerKey.contains("census");
        boolean aliasLooksCodeLike = aliasKey.contains("code") || aliasKey.contains("census");
        return headerLooksCodeLike && !aliasLooksCodeLike;
    }

    private String cleanCell(Row row, int colIndex) {
        if (colIndex < 0 || row == null || row.getCell(colIndex) == null) {
            return null;
        }
        return cleanText(dataFormatter.formatCellValue(row.getCell(colIndex)));
    }

    private String cleanText(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll("\\s+", " ");
        return value.isBlank() ? null : value;
    }

    private String composeKey(Long parentId, String name) {
        return parentId + "|" + nameKey(name);
    }

    private String nameKey(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return "";
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String headerKey(String value) {
        String key = nameKey(value);
        return key.replaceAll("[^a-z0-9]", "");
    }

    private String generateStateCode(String stateName, Set<String> usedStateCodes) {
        String base = "ST-" + shortHash(stateName);
        if (!usedStateCodes.contains(base)) {
            return base;
        }
        int suffix = 1;
        while (usedStateCodes.contains(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private String shortHash(String value) {
        String normalized = nameKey(value);
        int hash = Math.abs(normalized.hashCode());
        String hex = Integer.toHexString(hash).toUpperCase(Locale.ROOT);
        return hex.length() <= 6 ? hex : hex.substring(0, 6);
    }

    private Path resolveImportFile(String fileName) {
        List<Path> candidates = List.of(
                Paths.get(fileName),
                Paths.get("..", fileName),
                Paths.get("..", "..", fileName),
                Paths.get("..", "CitizenSync_Source", fileName),
                Paths.get(System.getProperty("user.dir"), fileName),
                Paths.get(System.getProperty("user.dir"), "..", fileName),
                Paths.get(System.getProperty("user.dir"), "..", "CitizenSync_Source", fileName)
        );

        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.exists(absolute) && Files.isRegularFile(absolute)) {
                log.debug("Resolved import file '{}' to {}", fileName, absolute);
                return absolute;
            }
        }
        log.debug("Could not resolve import file '{}'. Tried: {}", fileName, candidates);
        return null;
    }
}
