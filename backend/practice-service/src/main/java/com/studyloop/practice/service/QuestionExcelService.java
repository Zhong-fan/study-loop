package com.studyloop.practice.service;

import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.util.JsonConverter;
import com.studyloop.practice.vo.ImportErrorVO;
import com.studyloop.practice.vo.ImportResultVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Adapted from quizzy (MIT) — https://github.com/hhh666hhh666/quizzy
 * Copyright (c) 2026 Zihui Luo
 * Original: quizzy-server/.../service/QuestionImportService.java
 *
 * 按本项目题型、分库和“整批失败可回滚”改写；未保留分类/标签/试卷模型。
 */
@Service
public class QuestionExcelService {

    private static final String[] HEADERS = {
            "题型", "题干", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "答案", "解析", "难度"
    };

    private static final int MAX_ROWS = 2000;

    private final QuestionService questionService;
    private final QuestionMapper questionMapper;

    public QuestionExcelService(QuestionService questionService, QuestionMapper questionMapper) {
        this.questionService = questionService;
        this.questionMapper = questionMapper;
    }

    @Transactional
    public ImportResultVO importExcel(InputStream inputStream, Long courseId, Long chapterId, boolean skipInvalid) {
        List<QuestionCreateRequest> requests = new ArrayList<>();
        List<Integer> rowNumbers = new ArrayList<>();
        List<ImportErrorVO> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = readHeader(sheet);
            DataFormatter formatter = new DataFormatter();

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String stem = cell(row, columns, "题干", formatter);
                if (stem == null || stem.isBlank()) {
                    continue;
                }
                if (requests.size() >= MAX_ROWS) {
                    errors.add(new ImportErrorVO(i + 1, stem, "单次最多导入 " + MAX_ROWS + " 行"));
                    break;
                }
                try {
                    QuestionCreateRequest request = toRequest(row, columns, formatter, courseId, chapterId);
                    questionService.validateForSave(request);
                    requests.add(request);
                    rowNumbers.add(i + 1);
                } catch (Exception e) {
                    errors.add(new ImportErrorVO(i + 1, stem, messageOf(e)));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 Excel 失败：" + messageOf(e));
        }

        if (requests.isEmpty() && errors.isEmpty()) {
            throw new IllegalArgumentException("文件里没有可导入的题目");
        }
        if (!errors.isEmpty() && !skipInvalid) {
            return ImportResultVO.aborted(requests.size() + errors.size(), errors);
        }

        int success = 0;
        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            try {
                createdIds.add(questionService.create(requests.get(i)));
                success++;
            } catch (Exception e) {
                errors.add(new ImportErrorVO(rowNumbers.get(i), requests.get(i).getStem(), messageOf(e)));
                if (!skipInvalid) {
                    // 事务回滚：整批不落库
                    throw new IllegalStateException("第 " + rowNumbers.get(i) + " 行写入失败：" + messageOf(e));
                }
            }
        }
        return ImportResultVO.done(requests.size() + errors.size(), success, errors, createdIds);
    }

    public void writeTemplate(OutputStream outputStream) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目");
            writeHeader(workbook, sheet);

            Row example = sheet.createRow(1);
            String[] values = {
                    "单选", "分页存储管理中，页表的作用是什么？",
                    "记录页号到物理块号的映射", "记录进程状态", "记录文件目录", "记录中断向量",
                    "", "", "A", "页表用于把逻辑地址的页号映射到内存物理块号", "2"
            };
            for (int i = 0; i < values.length; i++) {
                example.createCell(i).setCellValue(values[i]);
            }

            Sheet note = workbook.createSheet("说明");
            String[][] rows = {
                    {"字段", "说明"},
                    {"题型", "单选 / 多选 / 判断 / 填空 / 主观 / 图片"},
                    {"选项A-F", "仅选择题填写，最多 6 个；判断题和填空题留空"},
                    {"答案", "单选填 A；多选填 A、C；判断题填 正确 或 错误；填空题多个答案用、分隔"},
                    {"难度", "1-5，留空默认 3"},
                    {"导入方式", "默认整批导入：任何一行错误都不会写入；可选择跳过错误行"}
            };
            for (int r = 0; r < rows.length; r++) {
                Row row = note.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            note.setColumnWidth(0, 14 * 256);
            note.setColumnWidth(1, 80 * 256);

            workbook.write(outputStream);
        } catch (Exception e) {
            throw new IllegalStateException("生成模板失败：" + messageOf(e));
        }
    }

    public void exportExcel(Long courseId, String type, Integer status, OutputStream outputStream) {
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.eq("user_id", com.studyloop.common.context.UserContext.requireUserId());
        if (courseId != null) {
            wrapper.eq("course_id", courseId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq("type", type);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        wrapper.last("limit 5000");
        List<Question> questions = questionMapper.selectList(wrapper);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目");
            writeHeader(workbook, sheet);
            int rowIndex = 1;
            for (Question question : questions) {
                Row row = sheet.createRow(rowIndex++);
                List<String> options = JsonConverter.toList(question.getOptions());
                writeCell(row, 0, typeText(question.getType()));
                writeCell(row, 1, question.getStem());
                for (int i = 0; i < 6; i++) {
                    writeCell(row, 2 + i, i < options.size() ? options.get(i) : "");
                }
                writeCell(row, 8, String.join("、", JsonConverter.toList(question.getAnswer())));
                writeCell(row, 9, question.getAnalysis());
                writeCell(row, 10, question.getDifficulty() == null ? "" : String.valueOf(question.getDifficulty()));
            }
            workbook.write(outputStream);
        } catch (Exception e) {
            throw new IllegalStateException("导出失败：" + messageOf(e));
        }
    }

    private Map<String, Integer> readHeader(Sheet sheet) {
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) {
            throw new IllegalArgumentException("缺少表头");
        }
        Map<String, Integer> columns = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim();
            if (!name.isEmpty()) {
                columns.put(name, cell.getColumnIndex());
            }
        }
        for (String required : new String[]{"题型", "题干", "答案"}) {
            if (!columns.containsKey(required)) {
                throw new IllegalArgumentException("缺少列：" + required);
            }
        }
        return columns;
    }

    private QuestionCreateRequest toRequest(Row row, Map<String, Integer> columns,
                                            DataFormatter formatter, Long courseId, Long chapterId) {
        String type = normalizeType(cell(row, columns, "题型", formatter));
        List<String> options = new ArrayList<>();
        for (String key : new String[]{"选项A", "选项B", "选项C", "选项D", "选项E", "选项F"}) {
            String value = cell(row, columns, key, formatter);
            if (value != null && !value.isBlank()) {
                options.add(value.replaceFirst("^[A-Fa-f][.、．:：)）]\\s*", "").trim());
            }
        }
        List<String> answer = normalizeAnswer(type, cell(row, columns, "答案", formatter));

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setCourseId(courseId);
        request.setChapterId(chapterId);
        request.setType(type);
        request.setStem(cell(row, columns, "题干", formatter).trim());
        request.setOptions(options);
        request.setAnswer(answer);
        request.setAnalysis(cell(row, columns, "解析", formatter));
        String difficulty = cell(row, columns, "难度", formatter);
        if (difficulty != null && !difficulty.isBlank()) {
            request.setDifficulty((int) Double.parseDouble(difficulty.trim()));
        }
        return request;
    }

    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("题型不能为空");
        }
        String text = raw.trim().toUpperCase();
        return switch (text) {
            case "单选", "SINGLE" -> "SINGLE";
            case "多选", "MULTIPLE" -> "MULTIPLE";
            case "判断", "JUDGE" -> "JUDGE";
            case "填空", "BLANK" -> "BLANK";
            case "简答", "主观", "SUBJECTIVE" -> "SUBJECTIVE";
            case "图片", "IMAGE" -> "IMAGE";
            default -> throw new IllegalArgumentException("未知题型：" + raw);
        };
    }

    private List<String> normalizeAnswer(String type, String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("答案不能为空");
        }
        String text = raw.trim();
        List<String> result = new ArrayList<>();
        if ("JUDGE".equals(type)) {
            if (text.contains("错") || "false".equalsIgnoreCase(text) || "F".equalsIgnoreCase(text)) {
                result.add("错误");
            } else if (text.contains("对") || text.contains("正确") || "true".equalsIgnoreCase(text)) {
                result.add("正确");
            } else {
                throw new IllegalArgumentException("判断题答案只能是 正确 / 错误");
            }
            return result;
        }
        if ("SINGLE".equals(type) || "MULTIPLE".equals(type)) {
            for (char c : text.toUpperCase().toCharArray()) {
                if (c >= 'A' && c <= 'F') {
                    result.add(String.valueOf(c));
                }
            }
            if (result.isEmpty()) {
                throw new IllegalArgumentException("选择题答案应为 A-F 字母");
            }
            return result;
        }
        for (String item : text.split("[、;；|]")) {
            if (!item.isBlank()) {
                result.add(item.trim());
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("答案不能为空");
        }
        return result;
    }

    private String cell(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer index = columns.get(name);
        if (row == null || index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        return cell == null ? null : formatter.formatCellValue(cell).trim();
    }

    private void writeHeader(Workbook workbook, Sheet sheet) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, (i == 1 ? 50 : 16) * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private void writeCell(Row row, int index, String value) {
        row.createCell(index).setCellValue(value == null ? "" : value);
    }

    private String typeText(String type) {
        return switch (type) {
            case "SINGLE" -> "单选";
            case "MULTIPLE" -> "多选";
            case "JUDGE" -> "判断";
            case "BLANK" -> "填空";
            case "SUBJECTIVE" -> "主观";
            case "IMAGE" -> "图片";
            default -> type;
        };
    }

    private String messageOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
