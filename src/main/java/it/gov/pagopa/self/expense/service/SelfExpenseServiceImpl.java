package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.connector.FileStorageAsyncConnector;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.dto.ReportExcelDTO;
import it.gov.pagopa.self.expense.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.model.SelfDeclarationText;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.self.expense.repository.SelfDeclarationTextRepository;
import it.gov.pagopa.self.expense.utils.Utils;
import it.gov.pagopa.self.expense.utils.excel.ExcelPOIHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class SelfExpenseServiceImpl implements SelfExpenseService {

    private final AnprInfoRepository anprInfoRepository;
    private final ExpenseDataRepository expenseDataRepository;
    private final ExceptionMap exceptionMap;
    private final CacheService cacheService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final RtdProducer rtdProducer;
    private final FileStorageAsyncConnector fileStorageConnector;
    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final SelfDeclarationTextRepository selfDeclarationTextRepository;


    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository, ExceptionMap exceptionMap, ExpenseDataRepository expenseDataRepository, CacheService cacheService, UserFiscalCodeService userFiscalCodeService, RtdProducer rtdProducer, FileStorageAsyncConnector fileStorageConnector, OnboardingFamiliesRepository onboardingFamiliesRepository,SelfDeclarationTextRepository selfDeclarationTextRepository) {
        this.anprInfoRepository = anprInfoRepository;
        this.expenseDataRepository = expenseDataRepository;
        this.exceptionMap = exceptionMap;
        this.cacheService = cacheService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.rtdProducer = rtdProducer;
        this.fileStorageConnector = fileStorageConnector;
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.selfDeclarationTextRepository = selfDeclarationTextRepository;
    }

    @Override
    public Mono<ChildResponseDTO> getChildForUserId(String milAuthToken) {
        log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Fetching child information for user with token: {}", milAuthToken);
        return cacheService.getFromCache(milAuthToken)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                        Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .flatMap(userFiscalCodeService::getUserId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                        Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .flatMap(userId -> {
                    log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Fetching ANPR info for userId: {}", userId);
                    return anprInfoRepository.findByUserId(userId)
                            .switchIfEmpty(Mono.error(exceptionMap.throwException(
                                    Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                                    Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                            .map(anprInfo -> {
                                ChildResponseDTO childResponseDTO = new ChildResponseDTO();
                                childResponseDTO.setChildList(anprInfo.getChildList());
                                childResponseDTO.setUserId(userId);
                                log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Child information retrieved for userId: {}", userId);
                                return childResponseDTO;
                            });
                });
    }

    @Override
    public Mono<Void> saveExpenseData(List<FilePart> fileList, ExpenseDataDTO expenseData) {
        log.info("[SELF-EXPENSE-SERVICE][SAVE] Saving expense data for user: {}", expenseData.getFiscalCode());
        return validateFiles(fileList)
                .flatMap(validFiles -> saveFiles(expenseData.getFiscalCode(), validFiles))
                .flatMap(savedFiles -> retrieveFiscalCodeAndSaveToDatabase(expenseData, savedFiles))
                .flatMap(savedData  -> rtdProducer.scheduleMessage(expenseData))
                .doOnSuccess(result -> log.info("Expense data saved successfully for user: {}", expenseData.getFiscalCode()))
                .onErrorResume(e -> handleSaveError(expenseData.getFiscalCode(), fileList, e));
    }

    @Override
    public Mono<ResponseEntity<byte[]>> generateReportExcel(String initiativeId)  {

        List<ReportExcelDTO> dataForReport = extractDataForReport(initiativeId);

        if( !dataForReport.isEmpty()){
            ExcelPOIHelper excelHelper = new ExcelPOIHelper();
            try {
                byte[] excelBytes = excelHelper.genExcel(ReportExcelDTO.headerName, Utils.generateRowValuesForReport(dataForReport));
                return Mono.just(ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=ReportCentriEstivi.xlsx")
                        .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                        .body(excelBytes));
            } catch (IOException e) {
                log.error("Exception in generateReportExcel", e);
                return Mono.just(ResponseEntity.internalServerError().build());
            }

        }else{
            return Mono.just(ResponseEntity.notFound().build());
        }

    }

    @Override
    public Mono<ResponseEntity<byte[]>> downloadExpenseFile(String initiativeId) {

        Mono<Map<String, String>> fileNameMap = extractFileNameList(initiativeId);
        return fileNameMap
                .flatMapIterable(Map::entrySet)
                .flatMap(entry -> {
                    String originalFileName = entry.getKey();
                    String newFileName = entry.getValue();

                    return fileStorageConnector.downloadFile(originalFileName)
                            .collectList()
                            .flatMap(byteBuffers -> {
                                // Unisci i ByteBuffer in un array di byte
                                byte[] fileData = ByteBuffer.allocate(byteBuffers.stream().mapToInt(ByteBuffer::remaining).sum())
                                        .put(byteBuffers.stream().reduce(ByteBuffer::put).orElse(ByteBuffer.allocate(0)))
                                        .array();

                                // Crea un ZipEntry per il file
                                return Mono.just(Map.entry(new ZipEntry(newFileName), fileData));
                            });
                })
                .collectList()
                .flatMap(zipEntriesAndData -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                         ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

                        for (var entry : zipEntriesAndData) {
                            ZipEntry zipEntry = entry.getKey();
                            byte[] data = entry.getValue();

                            zipOutputStream.putNextEntry(zipEntry);
                            zipOutputStream.write(data);
                            zipOutputStream.closeEntry();
                        }

                        zipOutputStream.finish();
                        byte[] zipBytes = byteArrayOutputStream.toByteArray();

                        return Mono.just(ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=expenseFiles.zip")
                                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                                .body(zipBytes));
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException("Error creating ZIP file", e));
                    }
                });

    }



    public Mono<Map<String, String>> extractFileNameList(String initiativeId) {
        // Fetch all expense data for the initiative
        List<ReportExcelDTO> data = extractDataForReport(initiativeId);

        List<ExpenseData> expenseDataList = new ArrayList<>();

        Map<String, String> expenseFileMap = new HashMap<>(); // <[storedFilePath], [fileName With Other info]>

        for (ReportExcelDTO reportExcelDTO : data) {
            expenseDataList.addAll(reportExcelDTO.getExpenseDataList());
        }

        // Create a Flux from the expenseDataList to process each ExpenseData
        return Flux.fromIterable(expenseDataList)
                .flatMap(expenseData ->
                        userFiscalCodeService.getUserFiscalCode(expenseData.getUserId())
                                .map(cf -> {
                                    for (String filename : expenseData.getFilesName()) {
                                        String storedPath = String.format("%s/%s", expenseData.getUserId(), filename);
                                        String downloadName = String.format("%s_%s_%s_%s", cf, expenseData.getName(),
                                                expenseData.getSurname(), filename);
                                        expenseFileMap.put(storedPath, downloadName);
                                    }
                                    return expenseFileMap;
                                })
                )
                .collectList()
                .map(list -> expenseFileMap); // Return the final map
    }

    public List<ReportExcelDTO> extractDataForReport(String initiativeId) {
        List<ReportExcelDTO> excelReportDTOList = new ArrayList<>();

        onboardingFamiliesRepository.findByInitiativeId(initiativeId)
                .filter(family -> OnboardingFamilyEvaluationStatus.ONBOARDING_OK.equals(family.getStatus()))
                .collectList()
                .flatMapMany(Flux::fromIterable)
                .flatMap(family -> {
                    ReportExcelDTO excelReportDTO = new ReportExcelDTO();


                    return anprInfoRepository.findByFamilyId(family.getFamilyId())
                            .flatMap(anprInfo -> {
                                return getUserFiscalCodeFromListId(family.getMemberIds())
                                        .flatMap(cfListString -> {
                                            excelReportDTO.set_2_CF_compNucleo(cfListString);
                                            return userFiscalCodeService.getUserFiscalCode(anprInfo.getUserId())
                                                    .doOnNext(excelReportDTO::set_0_cfGenTutore)
                                                    .then(Mono.just(anprInfo));
                                        });
                            })
                            .flatMap(anprInfo -> {
                                excelReportDTO.set_4_N_figliMinori(String.valueOf(anprInfo.getChildList().size()));
                                excelReportDTO.set_3_N_minoriNucleo(String.valueOf(anprInfo.getUnderAgeNumber()));

                                return selfDeclarationTextRepository.findById(SelfDeclarationText.buildId(family.getInitiativeId(), anprInfo.getUserId()))
                                        .flatMap(selfDeclaration -> {
                                            excelReportDTO.set_1_dichiarazioni(selfDeclaration != null ? Utils.formatDeclaration(selfDeclaration.getSelfDeclarationTextValues()) : "");
                                            return expenseDataRepository.findByUserId(anprInfo.getUserId()).collectList();
                                        })
                                        .map(expenseDataList -> {
                                            excelReportDTO.setExpenseDataList(expenseDataList);
                                            return excelReportDTO;
                                        });
                            });
                })
                .doOnNext(excelReportDTOList::add)
                .blockLast();

        return excelReportDTOList;
    }



    private Mono<String> getUserFiscalCodeFromListId(Set<String> ids) {
        return Flux.fromIterable(ids)
                .flatMap(userFiscalCodeService::getUserFiscalCode)
                .collectList()
                .map(fiscalCodes -> String.join("\n", fiscalCodes));
    }

    private Mono<List<FilePart>> validateFiles(List<FilePart> fileList) {
        return Flux.fromIterable(fileList)
                .flatMap(this::fileValidation)
                .collectList()
                .flatMap(validations -> {
                    if (validations.contains(Boolean.FALSE)) {
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.EXPENSE_DATA_FILE_VALIDATION,
                                Constants.ExceptionMessage.EXPENSE_DATA_FILE_VALIDATION));
                    }
                    return Mono.just(fileList);
                });
    }

    private Mono<List<FilePart>> saveFiles(String fiscalCode, List<FilePart> fileList) {
        return Flux.fromIterable(fileList)
                .flatMap(file -> storeFile(fiscalCode, file))
                .collectList()
                .flatMap(validations -> {
                    if (validations.contains(Boolean.FALSE)) {
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.EXPENSE_DATA_FILE_VALIDATION,
                                Constants.ExceptionMessage.EXPENSE_DATA_FILE_VALIDATION));
                    }
                    return Mono.just(fileList);
                });
    }

    private Mono<ExpenseData> retrieveFiscalCodeAndSaveToDatabase(ExpenseDataDTO expenseData, List<FilePart> fileList) {
        return userFiscalCodeService.getUserId(expenseData.getFiscalCode())
                .flatMap(fiscalCode ->
                        expenseDataRepository.save(ExpenseDataMapper.map(expenseData, fileList))
                                .onErrorResume(e -> Mono.error(exceptionMap.throwException(
                                        Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                                        Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB)))
                )
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.EXPENSE_DATA_FISCAL_CODE_NOT_FOUND,
                        Constants.ExceptionMessage.EXPENSE_DATA_FISCAL_CODE_NOT_FOUND)));
    }

    private Mono<Void> handleSaveError(String fiscalCode, List<FilePart> fileList, Throwable e) {
        log.error("[SELF-EXPENSE-SERVICE][SAVE] Error saving expense data for user: {}", fiscalCode, e);
        return deleteUploadedFiles(fiscalCode, fileList)
                .then(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                        Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB)));
    }

    public Mono<Boolean> fileValidation(FilePart file) {

        Mono<Boolean> isEmpty = file.content()
                .map(DataBuffer::readableByteCount)
                .reduce(Integer::sum)
                .map(size -> size == 0);

        Mono<Boolean> isValidContentType = Mono.justOrEmpty(file.headers().getContentType())
                .map(contentType -> Constants.CONTENT_TYPES.contains(contentType.toString()))
                .defaultIfEmpty(false);

        return Mono.zip(isEmpty, isValidContentType)
                .map(tuple -> {
                    boolean empty = tuple.getT1();
                    boolean validContentType = tuple.getT2();

                    if (empty) {
                        log.info("[SELF-EXPENSE-SERVICE][FILE-VALIDATION] File is empty");
                    }

                    if (!validContentType) {
                        log.info("[SELF-EXPENSE-SERVICE][FILE-VALIDATION] ContentType not accepted: {}", Objects.requireNonNull(file.headers().getContentType()));
                    }

                    return !empty && validContentType;
                });
    }

    public Mono<Boolean> storeFile(String fiscalCode, FilePart filePart) {
        Flux<DataBuffer> dataBufferFlux = filePart.content();
        Flux<ByteBuffer> byteBufferFlux = dataBufferFlux.flatMapSequential(dataBuffer -> {
            Iterator<ByteBuffer> iterator = dataBuffer.readableByteBuffers();
            Stream<ByteBuffer> stream = StreamSupport.stream(((Iterable<ByteBuffer>) () -> iterator).spliterator(), false);
            return Flux.fromStream(stream);
        });
        return fileStorageConnector.uploadFile(
                    byteBufferFlux,
                    String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, filePart.filename()),
                    Objects.requireNonNull(filePart.headers().getContentType()).toString());


    }

    private Mono<Void> deleteUploadedFiles(String fiscalCode, List<FilePart> files) {
        return Flux.fromIterable(files)
                .flatMap(file ->
                        fileStorageConnector.delete(String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, file.filename()))
                                .flatMap(response -> {
                                    if (Boolean.TRUE.equals(response)) {
                                        log.info("[SELF-EXPENSE-SERVICE][REVERT-UPLOAD] File {} deleted from storage", file.filename());
                                    } else {
                                        log.info("[SELF-EXPENSE-SERVICE][REVERT-UPLOAD] Failed to delete file {} from storage", file.filename());
                                    }
                                    return Mono.empty();
                                })
                )
                .then();
    }
}
