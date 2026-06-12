package com.smarthospital.core.export;

import com.smarthospital.modules.analytics.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportUtil {

    private ExcelExportUtil() {}

    // ═══════════════════════════════════════════════════════════════════════
    // Style kit — created once per workbook to stay under the 64k limit
    // ═══════════════════════════════════════════════════════════════════════
    private static final class Styles {
        final CellStyle title;
        final CellStyle meta;
        final CellStyle sectionHeader;
        final CellStyle colHeader;
        final CellStyle kpiLabel;
        final CellStyle kpiValue;
        final CellStyle dataEven;
        final CellStyle dataOdd;
        final CellStyle numEven;
        final CellStyle numOdd;
        final CellStyle currency;
        final CellStyle currencyOdd;
        final CellStyle pctStyle;

        Styles(XSSFWorkbook wb) {
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(new XSSFColor(hexToBytes("#1677ff"), null));

            XSSFFont sectionFont = wb.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setColor(new XSSFColor(hexToBytes("#1677ff"), null));

            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(new XSSFColor(hexToBytes("#ffffff"), null));

            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 9);

            XSSFFont kpiNumFont = wb.createFont();
            kpiNumFont.setBold(true);
            kpiNumFont.setFontHeightInPoints((short) 14);

            XSSFFont labelFont = wb.createFont();
            labelFont.setFontHeightInPoints((short) 9);

            XSSFFont grayFont = wb.createFont();
            grayFont.setFontHeightInPoints((short) 8);
            grayFont.setColor(new XSSFColor(hexToBytes("#8c8c8c"), null));

            title = style(wb, s -> {
                s.setFont(titleFont);
                s.setAlignment(HorizontalAlignment.LEFT);
            });
            meta = style(wb, s -> {
                s.setFont(grayFont);
                s.setAlignment(HorizontalAlignment.LEFT);
            });
            sectionHeader = style(wb, s -> {
                s.setFont(sectionFont);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#1677ff"), null).getIndex());
                s.setBorderBottom(BorderStyle.MEDIUM);
                s.setAlignment(HorizontalAlignment.LEFT);
            });
            colHeader = style(wb, s -> {
                s.setFont(headerFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#1677ff"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setAlignment(HorizontalAlignment.LEFT);
                s.setWrapText(false);
            });
            kpiLabel = style(wb, s -> s.setFont(grayFont));
            kpiValue = style(wb, s -> s.setFont(kpiNumFont));
            dataEven = style(wb, s -> {
                s.setFont(labelFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#ffffff"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
            });
            dataOdd = style(wb, s -> {
                s.setFont(labelFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#f5f7fa"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
            });
            numEven = style(wb, s -> {
                s.setFont(boldFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#ffffff"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
                s.setAlignment(HorizontalAlignment.RIGHT);
                s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            });
            numOdd = style(wb, s -> {
                s.setFont(boldFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#f5f7fa"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
                s.setAlignment(HorizontalAlignment.RIGHT);
                s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            });
            currency = style(wb, s -> {
                s.setFont(boldFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#ffffff"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
                s.setAlignment(HorizontalAlignment.RIGHT);
                s.setDataFormat(wb.createDataFormat().getFormat("\"₹\"#,##0"));
            });
            currencyOdd = style(wb, s -> {
                s.setFont(boldFont);
                ((XSSFCellStyle) s).setFillForegroundColor(new XSSFColor(hexToBytes("#f5f7fa"), null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                s.setBorderBottom(BorderStyle.THIN);
                s.setBottomBorderColor(new XSSFColor(hexToBytes("#e8e8e8"), null).getIndex());
                s.setAlignment(HorizontalAlignment.RIGHT);
                s.setDataFormat(wb.createDataFormat().getFormat("\"₹\"#,##0"));
            });
            pctStyle = style(wb, s -> {
                s.setFont(boldFont);
                s.setAlignment(HorizontalAlignment.RIGHT);
                s.setDataFormat(wb.createDataFormat().getFormat("0.0%"));
            });
        }

        private static CellStyle style(XSSFWorkbook wb, java.util.function.Consumer<CellStyle> cfg) {
            CellStyle s = wb.createCellStyle();
            cfg.accept(s);
            return s;
        }

        private static byte[] hexToBytes(String hex) {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return new byte[]{
                (byte) Integer.parseInt(h.substring(0, 2), 16),
                (byte) Integer.parseInt(h.substring(2, 4), 16),
                (byte) Integer.parseInt(h.substring(4, 6), 16)
            };
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sheet-building helpers
    // ═══════════════════════════════════════════════════════════════════════

    private static void writeReportHeader(Sheet sheet, String title,
                                          String dateRange, String generatedBy,
                                          Styles st) {
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(22);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("SmartHospital — " + title);
        c0.setCellStyle(st.title);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellStyle(st.meta);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Period: " + dateRange + "   |   Generated by: " + generatedBy +
                        "   |   " + LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        c1.setCellStyle(st.meta);
        sheet.createRow(2); // blank spacer
    }

    private static int writeSectionTitle(Sheet sheet, int rowNum, String text, Styles st) {
        Row r = sheet.createRow(rowNum);
        r.setHeightInPoints(16);
        Cell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(st.sectionHeader);
        return rowNum + 1;
    }

    /** Writes a 2-column KPI block: label | value, returns next rowNum. */
    private static int writeKpiBlock(Sheet sheet, int rowNum, List<KpiEntry> kpis, Styles st) {
        for (KpiEntry k : kpis) {
            Row labelRow = sheet.createRow(rowNum++);
            labelRow.setHeightInPoints(14);
            Cell lbl = labelRow.createCell(0);
            lbl.setCellValue(k.label);
            lbl.setCellStyle(st.kpiLabel);

            Row valRow = sheet.createRow(rowNum++);
            valRow.setHeightInPoints(22);
            Cell val = valRow.createCell(0);
            if (k.numericValue != null) {
                val.setCellValue(k.numericValue);
                val.setCellStyle(k.isCurrency ? st.currency : st.numEven);
            } else {
                val.setCellValue(k.textValue);
                val.setCellStyle(st.kpiValue);
            }
            sheet.createRow(rowNum++); // mini spacer between KPIs
        }
        return rowNum;
    }

    /** Writes a labelled bar/count table: Name | numeric value. Returns next rowNum. */
    private static int writeNameValueTable(Sheet sheet, int rowNum,
                                           String[] headers, List<NameValuePoint> data,
                                           boolean isCurrency, Styles st) {
        Row hdr = sheet.createRow(rowNum++);
        hdr.setHeightInPoints(15);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(st.colHeader);
        }
        boolean odd = false;
        for (NameValuePoint pt : data) {
            Row r = sheet.createRow(rowNum++);
            r.setHeightInPoints(14);
            Cell name = r.createCell(0);
            name.setCellValue(pt.name());
            name.setCellStyle(odd ? st.dataOdd : st.dataEven);

            Cell val = r.createCell(1);
            val.setCellValue(pt.value());
            val.setCellStyle(isCurrency ? (odd ? st.currencyOdd : st.currency)
                                        : (odd ? st.numOdd : st.numEven));
            odd = !odd;
        }
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 5000);
        return rowNum;
    }

    /** Writes a trend table: Label | numeric value. Returns next rowNum. */
    private static int writeTrendTable(Sheet sheet, int rowNum,
                                       String[] headers, List<TrendPoint> data,
                                       boolean isCurrency, Styles st) {
        Row hdr = sheet.createRow(rowNum++);
        hdr.setHeightInPoints(15);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(st.colHeader);
        }
        boolean odd = false;
        for (TrendPoint pt : data) {
            Row r = sheet.createRow(rowNum++);
            r.setHeightInPoints(14);
            Cell lbl = r.createCell(0);
            lbl.setCellValue(pt.label());
            lbl.setCellStyle(odd ? st.dataOdd : st.dataEven);

            Cell val = r.createCell(1);
            val.setCellValue(pt.value());
            val.setCellStyle(isCurrency ? (odd ? st.currencyOdd : st.currency)
                                        : (odd ? st.numOdd : st.numEven));
            odd = !odd;
        }
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 5000);
        return rowNum;
    }

    private static byte[] toBytes(XSSFWorkbook wb) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel workbook", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Section builders
    // ═══════════════════════════════════════════════════════════════════════

    public static byte[] buildExecutive(ExecutiveDashboardResponse d,
                                        String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);

            // ── Sheet 1: Summary (all KPIs) ───────────────────────────────
            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 8000);
            summary.setColumnWidth(1, 6000);
            writeReportHeader(summary, "Executive Dashboard", dateRange, generatedBy, st);

            int row = 3;
            row = writeSectionTitle(summary, row, "Key Performance Indicators", st);
            Row hdr = summary.createRow(row++);
            hdr.setHeightInPoints(15);
            Cell hMetric = hdr.createCell(0); hMetric.setCellValue("Metric"); hMetric.setCellStyle(st.colHeader);
            Cell hValue  = hdr.createCell(1); hValue.setCellValue("Value");   hValue.setCellStyle(st.colHeader);
            Cell hPeriod = hdr.createCell(2); hPeriod.setCellValue("Period"); hPeriod.setCellStyle(st.colHeader);

            Object[][] kpis = {
                {"Today's Revenue",      d.todayRevenue(),          "Today",      true},
                {"Month Revenue",        d.monthRevenue(),          "This Month", true},
                {"Total Patients",       d.totalPatients(),         "All Time",   false},
                {"Today's Appointments", d.todayAppointments(),     "Today",      false},
                {"Pending Payments",     d.pendingPayments(),       "Outstanding",true},
                {"Doctors Available",    d.doctorsAvailableToday(), "Today",      false},
                {"Current Admissions",   d.currentAdmissions(),     "IPD Active", false},
                {"Lab Tests Today",      d.labTestsToday(),         "Today",      false},
                {"Medicine Sales",       d.medicineSalesToday(),    "Today",      true},
                {"Inventory Alerts",     d.inventoryAlerts(),       "Items",      false},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++);
                r.setHeightInPoints(15);
                Cell name = r.createCell(0); name.setCellValue((String) k[0]);
                name.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell val = r.createCell(1);
                val.setCellValue(((Number) k[1]).doubleValue());
                val.setCellStyle(((Boolean) k[3]) ? (alt ? st.currencyOdd : st.currency)
                                                  : (alt ? st.numOdd : st.numEven));
                Cell period = r.createCell(2); period.setCellValue((String) k[2]);
                period.setCellStyle(alt ? st.dataOdd : st.dataEven);
                alt = !alt;
            }
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(2);

            // ── Sheet 2: Revenue by Source ────────────────────────────────
            Sheet revSource = wb.createSheet("Revenue by Source");
            revSource.setColumnWidth(0, 7000);
            writeReportHeader(revSource, "Revenue by Source", dateRange, generatedBy, st);
            row = writeSectionTitle(revSource, 3, "Revenue by Source", st);
            writeNameValueTable(revSource, row, new String[]{"Source", "Revenue (₹)"},
                d.revenueBySource(), true, st);

            // ── Sheet 3: Top Doctors ──────────────────────────────────────
            Sheet topDoctors = wb.createSheet("Top Doctors");
            topDoctors.setColumnWidth(0, 8000);
            writeReportHeader(topDoctors, "Top Doctors by Revenue", dateRange, generatedBy, st);
            row = writeSectionTitle(topDoctors, 3, "Top Doctors by Revenue", st);
            writeNameValueTable(topDoctors, row, new String[]{"Doctor", "Revenue (₹)"},
                d.topDoctors(), true, st);

            // ── Sheet 4: Dept Revenue ─────────────────────────────────────
            Sheet deptRev = wb.createSheet("Dept Revenue");
            deptRev.setColumnWidth(0, 7000);
            writeReportHeader(deptRev, "Revenue by Department", dateRange, generatedBy, st);
            row = writeSectionTitle(deptRev, 3, "Revenue by Department", st);
            writeNameValueTable(deptRev, row, new String[]{"Department", "Revenue (₹)"},
                d.departmentRevenue(), true, st);

            // ── Sheet 5: Daily Revenue Trend ──────────────────────────────
            Sheet revTrend = wb.createSheet("Revenue Trend");
            revTrend.setColumnWidth(0, 4000);
            writeReportHeader(revTrend, "Daily Revenue Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(revTrend, 3, "Daily Revenue Trend (Last 30 Days)", st);
            writeTrendTable(revTrend, row, new String[]{"Date", "Revenue (₹)"},
                d.revenueTrend(), true, st);

            // ── Sheet 6: Patient Growth ───────────────────────────────────
            Sheet patGrowth = wb.createSheet("Patient Growth");
            patGrowth.setColumnWidth(0, 4000);
            writeReportHeader(patGrowth, "Patient Growth", dateRange, generatedBy, st);
            row = writeSectionTitle(patGrowth, 3, "Patient Registration Growth (Monthly)", st);
            writeTrendTable(patGrowth, row, new String[]{"Month", "Registrations"},
                d.patientGrowth(), false, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Executive Excel", e);
        }
    }

    public static byte[] buildFinance(FinanceAnalyticsResponse d,
                                      String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 8000); summary.setColumnWidth(1, 6000);
            writeReportHeader(summary, "Financial Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Financial Summary", st);
            Object[][] kpis = {
                {"Total Revenue",         d.totalRevenue(),            "currency"},
                {"Total Expenses",        d.totalExpenses(),           "currency"},
                {"Net Profit",            d.netProfit(),               "currency"},
                {"Collection Efficiency", d.collectionEfficiencyPct(), "pct"},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell name = r.createCell(0); name.setCellValue((String) k[0]);
                name.setCellStyle(alt ? st.dataOdd : st.dataEven);
                double rawVal = ((Number) k[1]).doubleValue();
                Cell val = r.createCell(1);
                if ("currency".equals(k[2])) {
                    val.setCellValue(rawVal);
                    val.setCellStyle(alt ? st.currencyOdd : st.currency);
                } else if ("pct".equals(k[2])) {
                    val.setCellValue(rawVal / 100);
                    val.setCellStyle(st.pctStyle);
                } else {
                    val.setCellValue(rawVal);
                    val.setCellStyle(alt ? st.numOdd : st.numEven);
                }
                alt = !alt;
            }
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 5500);

            Sheet revSrc = wb.createSheet("Revenue by Source");
            revSrc.setColumnWidth(0, 7000);
            writeReportHeader(revSrc, "Revenue by Source", dateRange, generatedBy, st);
            row = writeSectionTitle(revSrc, 3, "Revenue by Source", st);
            writeNameValueTable(revSrc, row, new String[]{"Source", "Revenue (₹)"},
                d.revenueBySource(), true, st);

            Sheet revDoc = wb.createSheet("Revenue by Doctor");
            revDoc.setColumnWidth(0, 8000);
            writeReportHeader(revDoc, "Revenue by Doctor", dateRange, generatedBy, st);
            row = writeSectionTitle(revDoc, 3, "Revenue by Doctor (Top 10)", st);
            writeNameValueTable(revDoc, row, new String[]{"Doctor", "Revenue (₹)"},
                d.revenueByDoctor(), true, st);

            Sheet monthly = wb.createSheet("Monthly Comparison");
            monthly.setColumnWidth(0, 4000);
            writeReportHeader(monthly, "Monthly Comparison", dateRange, generatedBy, st);
            row = writeSectionTitle(monthly, 3, "Monthly Revenue Comparison", st);
            writeNameValueTable(monthly, row, new String[]{"Month", "Revenue (₹)"},
                d.monthlyComparison(), true, st);

            Sheet revTrend = wb.createSheet("Revenue Trend");
            revTrend.setColumnWidth(0, 4000);
            writeReportHeader(revTrend, "Daily Revenue Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(revTrend, 3, "Daily Revenue Trend", st);
            writeTrendTable(revTrend, row, new String[]{"Date", "Revenue (₹)"},
                d.dailyRevenue(), true, st);

            Sheet expTrend = wb.createSheet("Expense Trend");
            expTrend.setColumnWidth(0, 4000);
            writeReportHeader(expTrend, "Daily Expense Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(expTrend, 3, "Daily Expense Trend", st);
            writeTrendTable(expTrend, row, new String[]{"Date", "Expenses (₹)"},
                d.expenseTrend(), true, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Finance Excel", e);
        }
    }

    public static byte[] buildPatients(PatientAnalyticsResponse d,
                                       String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 7000); summary.setColumnWidth(1, 5000);
            writeReportHeader(summary, "Patient Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Patient Summary", st);
            Object[][] kpis = {
                {"Total Patients",   d.totalPatients()},
                {"New This Period",  d.newPatientsThisPeriod()},
                {"Returning",        d.returningPatients()},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell nm = r.createCell(0); nm.setCellValue((String) k[0]);
                nm.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell v = r.createCell(1); v.setCellValue(((Number) k[1]).doubleValue());
                v.setCellStyle(alt ? st.numOdd : st.numEven);
                alt = !alt;
            }
            // Retention rate
            Row retRow = summary.createRow(row++); retRow.setHeightInPoints(15);
            Cell retLbl = retRow.createCell(0); retLbl.setCellValue("Retention Rate");
            retLbl.setCellStyle(alt ? st.dataOdd : st.dataEven);
            Cell retVal = retRow.createCell(1); retVal.setCellValue(d.retentionRatePct() / 100);
            retVal.setCellStyle(st.pctStyle);
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 4500);

            Sheet gender = wb.createSheet("Gender");
            gender.setColumnWidth(0, 5000);
            writeReportHeader(gender, "Gender Distribution", dateRange, generatedBy, st);
            row = writeSectionTitle(gender, 3, "Gender Distribution", st);
            writeNameValueTable(gender, row, new String[]{"Gender", "Count"},
                d.genderDistribution(), false, st);

            Sheet age = wb.createSheet("Age Groups");
            age.setColumnWidth(0, 4000);
            writeReportHeader(age, "Age Distribution", dateRange, generatedBy, st);
            row = writeSectionTitle(age, 3, "Age Distribution", st);
            writeNameValueTable(age, row, new String[]{"Age Group", "Count"},
                d.ageDistribution(), false, st);

            Sheet blood = wb.createSheet("Blood Groups");
            blood.setColumnWidth(0, 4000);
            writeReportHeader(blood, "Blood Group Distribution", dateRange, generatedBy, st);
            row = writeSectionTitle(blood, 3, "Blood Group Distribution", st);
            writeNameValueTable(blood, row, new String[]{"Blood Group", "Count"},
                d.bloodGroupDistribution(), false, st);

            Sheet trend = wb.createSheet("Registration Trend");
            trend.setColumnWidth(0, 4000);
            writeReportHeader(trend, "Registration Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(trend, 3, "Monthly Registration Trend", st);
            writeTrendTable(trend, row, new String[]{"Month", "Registrations"},
                d.registrationTrend(), false, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Patients Excel", e);
        }
    }

    public static byte[] buildDoctors(DoctorAnalyticsResponse d,
                                      String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            // Leaderboard sheet
            Sheet leader = wb.createSheet("Leaderboard");
            leader.setColumnWidth(0, 8500); leader.setColumnWidth(1, 6000);
            leader.setColumnWidth(2, 4500); leader.setColumnWidth(3, 4500); leader.setColumnWidth(4, 4500);
            writeReportHeader(leader, "Doctor Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(leader, 3, "Doctor Leaderboard", st);
            Row hdr = leader.createRow(row++); hdr.setHeightInPoints(15);
            String[] hdrLabels = {"Doctor", "Specialization", "Appointments", "Revenue (₹)", "Utilization %"};
            for (int i = 0; i < hdrLabels.length; i++) {
                Cell c = hdr.createCell(i); c.setCellValue(hdrLabels[i]); c.setCellStyle(st.colHeader);
            }
            boolean alt = false;
            int rank = 1;
            for (DoctorAnalyticsResponse.DoctorStatEntry e : d.leaderboard()) {
                Row r = leader.createRow(row++); r.setHeightInPoints(14);
                CellStyle ds = alt ? st.dataOdd : st.dataEven;
                CellStyle ns = alt ? st.numOdd  : st.numEven;
                CellStyle cs = alt ? st.currencyOdd : st.currency;
                r.createCell(0).setCellValue(rank++ + ". " + e.doctorName()); r.getCell(0).setCellStyle(ds);
                r.createCell(1).setCellValue(e.specialization());             r.getCell(1).setCellStyle(ds);
                r.createCell(2).setCellValue(e.appointmentsCompleted());      r.getCell(2).setCellStyle(ns);
                r.createCell(3).setCellValue(e.revenueGenerated().doubleValue()); r.getCell(3).setCellStyle(cs);
                r.createCell(4).setCellValue(e.utilizationPct() / 100);       r.getCell(4).setCellStyle(st.pctStyle);
                alt = !alt;
            }

            Sheet revDoc = wb.createSheet("Revenue by Doctor");
            revDoc.setColumnWidth(0, 8000);
            writeReportHeader(revDoc, "Revenue by Doctor", dateRange, generatedBy, st);
            row = writeSectionTitle(revDoc, 3, "Revenue by Doctor (Top 10)", st);
            writeNameValueTable(revDoc, row, new String[]{"Doctor", "Revenue (₹)"},
                d.revenueByDoctor(), true, st);

            Sheet apptDoc = wb.createSheet("Appointments by Doctor");
            apptDoc.setColumnWidth(0, 8000);
            writeReportHeader(apptDoc, "Appointments by Doctor", dateRange, generatedBy, st);
            row = writeSectionTitle(apptDoc, 3, "Appointments by Doctor (Top 10)", st);
            writeNameValueTable(apptDoc, row, new String[]{"Doctor", "Appointments"},
                d.appointmentsByDoctor(), false, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Doctors Excel", e);
        }
    }

    public static byte[] buildAppointments(AppointmentAnalyticsResponse d,
                                           String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 7000); summary.setColumnWidth(1, 5000);
            writeReportHeader(summary, "Appointment Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Appointment Summary", st);
            Object[][] kpis = {
                {"Total",       d.totalAppointments()},
                {"Completed",   d.completed()},
                {"Cancelled",   d.cancelled()},
                {"No Show",     d.noShow()},
                {"Rescheduled", d.rescheduled()},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell nm = r.createCell(0); nm.setCellValue((String) k[0]); nm.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell v  = r.createCell(1); v.setCellValue(((Number) k[1]).doubleValue()); v.setCellStyle(alt ? st.numOdd : st.numEven);
                alt = !alt;
            }
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 4500);

            Sheet status = wb.createSheet("Status");
            status.setColumnWidth(0, 6000);
            writeReportHeader(status, "Status Distribution", dateRange, generatedBy, st);
            row = writeSectionTitle(status, 3, "Appointment Status Distribution", st);
            writeNameValueTable(status, row, new String[]{"Status", "Count"}, d.statusDistribution(), false, st);

            Sheet byDoc = wb.createSheet("By Doctor");
            byDoc.setColumnWidth(0, 8000);
            writeReportHeader(byDoc, "Appointments by Doctor", dateRange, generatedBy, st);
            row = writeSectionTitle(byDoc, 3, "Appointments by Doctor (Top 10)", st);
            writeNameValueTable(byDoc, row, new String[]{"Doctor", "Appointments"}, d.byDoctor(), false, st);

            Sheet byDept = wb.createSheet("By Department");
            byDept.setColumnWidth(0, 7000);
            writeReportHeader(byDept, "Appointments by Department", dateRange, generatedBy, st);
            row = writeSectionTitle(byDept, 3, "Appointments by Department", st);
            writeNameValueTable(byDept, row, new String[]{"Department", "Appointments"}, d.byDepartment(), false, st);

            Sheet trend = wb.createSheet("Daily Trend");
            trend.setColumnWidth(0, 4000);
            writeReportHeader(trend, "Daily Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(trend, 3, "Daily Appointment Trend", st);
            writeTrendTable(trend, row, new String[]{"Date", "Appointments"}, d.dailyTrend(), false, st);

            if (d.peakHoursHeatmap() != null && !d.peakHoursHeatmap().isEmpty()) {
                Sheet heatmap = wb.createSheet("Peak Hours");
                heatmap.setColumnWidth(0, 3000);
                heatmap.setColumnWidth(1, 4000);
                heatmap.setColumnWidth(2, 3500);
                writeReportHeader(heatmap, "Peak Hours Heatmap", dateRange, generatedBy, st);
                row = writeSectionTitle(heatmap, 3, "Peak Hours by Weekday", st);
                Row hdr = heatmap.createRow(row++); hdr.setHeightInPoints(15);
                for (int ci = 0; ci < 3; ci++) {
                    Cell c = hdr.createCell(ci);
                    c.setCellValue(new String[]{"Hour", "Weekday", "Count"}[ci]);
                    c.setCellStyle(st.colHeader);
                }
                boolean hmAlt = false;
                for (AppointmentAnalyticsResponse.HeatmapCell cell : d.peakHoursHeatmap()) {
                    Row r = heatmap.createRow(row++); r.setHeightInPoints(15);
                    Cell h = r.createCell(0); h.setCellValue(cell.hour() + ":00"); h.setCellStyle(hmAlt ? st.dataOdd : st.dataEven);
                    Cell w = r.createCell(1); w.setCellValue(cell.weekday()); w.setCellStyle(hmAlt ? st.dataOdd : st.dataEven);
                    Cell ct = r.createCell(2); ct.setCellValue(cell.count()); ct.setCellStyle(hmAlt ? st.numOdd : st.numEven);
                    hmAlt = !hmAlt;
                }
            }

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Appointments Excel", e);
        }
    }

    public static byte[] buildPharmacy(PharmacyAnalyticsResponse d,
                                       String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 7000); summary.setColumnWidth(1, 5000);
            writeReportHeader(summary, "Pharmacy Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Pharmacy Summary", st);
            Object[][] kpis = {
                {"Total Revenue",    d.totalMedicineRevenue(), true},
                {"Bills Issued",     d.totalBillsIssued(),     false},
                {"Low Stock Alerts", d.lowStockAlerts(),       false},
                {"Expiry Alerts",    d.expiryAlerts(),         false},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell nm = r.createCell(0); nm.setCellValue((String) k[0]); nm.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell v  = r.createCell(1); v.setCellValue(((Number) k[1]).doubleValue());
                v.setCellStyle(((Boolean) k[2]) ? (alt ? st.currencyOdd : st.currency) : (alt ? st.numOdd : st.numEven));
                alt = !alt;
            }
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 5500);

            Sheet topMed = wb.createSheet("Top Medicines");
            topMed.setColumnWidth(0, 9000);
            writeReportHeader(topMed, "Top Medicines", dateRange, generatedBy, st);
            row = writeSectionTitle(topMed, 3, "Top Medicines by Revenue", st);
            writeNameValueTable(topMed, row, new String[]{"Medicine", "Revenue (₹)"}, d.topMedicinesByRevenue(), true, st);

            Sheet byCat = wb.createSheet("Revenue by Category");
            byCat.setColumnWidth(0, 7000);
            writeReportHeader(byCat, "Revenue by Category", dateRange, generatedBy, st);
            row = writeSectionTitle(byCat, 3, "Revenue by Category", st);
            writeNameValueTable(byCat, row, new String[]{"Category", "Revenue (₹)"}, d.revenueByCategory(), true, st);

            Sheet stock = wb.createSheet("Stock Health");
            stock.setColumnWidth(0, 6000);
            writeReportHeader(stock, "Stock Health", dateRange, generatedBy, st);
            row = writeSectionTitle(stock, 3, "Stock Health Distribution", st);
            writeNameValueTable(stock, row, new String[]{"Status", "Items"}, d.stockHealthDistribution(), false, st);

            Sheet trend = wb.createSheet("Revenue Trend");
            trend.setColumnWidth(0, 4000);
            writeReportHeader(trend, "Revenue Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(trend, 3, "Daily Revenue Trend", st);
            writeTrendTable(trend, row, new String[]{"Date", "Revenue (₹)"}, d.revenueTrend(), true, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Pharmacy Excel", e);
        }
    }

    public static byte[] buildLaboratory(LaboratoryAnalyticsResponse d,
                                         String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 7000); summary.setColumnWidth(1, 5000);
            writeReportHeader(summary, "Laboratory Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Laboratory Summary", st);
            Object[][] kpis = {
                {"Tests Performed", d.totalTestsPerformed(), false},
                {"Total Revenue",   d.totalRevenue(),        true},
                {"Pending Reports", d.pendingReports(),      false},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell nm = r.createCell(0); nm.setCellValue((String) k[0]); nm.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell v  = r.createCell(1); v.setCellValue(((Number) k[1]).doubleValue());
                v.setCellStyle(((Boolean) k[2]) ? (alt ? st.currencyOdd : st.currency) : (alt ? st.numOdd : st.numEven));
                alt = !alt;
            }
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 5500);

            Sheet topTests = wb.createSheet("Top Tests");
            topTests.setColumnWidth(0, 9000);
            writeReportHeader(topTests, "Top Tests", dateRange, generatedBy, st);
            row = writeSectionTitle(topTests, 3, "Top Tests by Volume", st);
            writeNameValueTable(topTests, row, new String[]{"Test", "Count"}, d.topTests(), false, st);

            Sheet status = wb.createSheet("Status");
            status.setColumnWidth(0, 6000);
            writeReportHeader(status, "Status", dateRange, generatedBy, st);
            row = writeSectionTitle(status, 3, "Tests by Status", st);
            writeNameValueTable(status, row, new String[]{"Status", "Count"}, d.statusDistribution(), false, st);

            Sheet referral = wb.createSheet("Referrals");
            referral.setColumnWidth(0, 6000);
            writeReportHeader(referral, "Referrals", dateRange, generatedBy, st);
            row = writeSectionTitle(referral, 3, "Tests by Referral Source", st);
            writeNameValueTable(referral, row, new String[]{"Source", "Count"}, d.byDepartmentReferral(), false, st);

            Sheet trend = wb.createSheet("Daily Trend");
            trend.setColumnWidth(0, 4000);
            writeReportHeader(trend, "Daily Tests Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(trend, 3, "Daily Tests Trend", st);
            writeTrendTable(trend, row, new String[]{"Date", "Tests"}, d.dailyTestsTrend(), false, st);

            Sheet revTrend = wb.createSheet("Revenue Trend");
            revTrend.setColumnWidth(0, 4000);
            writeReportHeader(revTrend, "Revenue Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(revTrend, 3, "Daily Revenue Trend", st);
            writeTrendTable(revTrend, row, new String[]{"Date", "Revenue (₹)"}, d.revenueTrend(), true, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Laboratory Excel", e);
        }
    }

    public static byte[] buildInventory(InventoryAnalyticsResponse d,
                                        String dateRange, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            int row;

            Sheet summary = wb.createSheet("Summary");
            summary.setColumnWidth(0, 7000); summary.setColumnWidth(1, 5000);
            writeReportHeader(summary, "Inventory Analytics", dateRange, generatedBy, st);
            row = writeSectionTitle(summary, 3, "Inventory Summary", st);
            Object[][] kpis = {
                {"Total Stock Value", d.totalStockValue(),  true},
                {"Total Items",       d.totalItems(),       false},
                {"Low Stock Items",   d.lowStockItems(),    false},
                {"Out of Stock",      d.outOfStockItems(),  false},
            };
            boolean alt = false;
            for (Object[] k : kpis) {
                Row r = summary.createRow(row++); r.setHeightInPoints(15);
                Cell nm = r.createCell(0); nm.setCellValue((String) k[0]); nm.setCellStyle(alt ? st.dataOdd : st.dataEven);
                Cell v  = r.createCell(1); v.setCellValue(((Number) k[1]).doubleValue());
                v.setCellStyle(((Boolean) k[2]) ? (alt ? st.currencyOdd : st.currency) : (alt ? st.numOdd : st.numEven));
                alt = !alt;
            }
            summary.autoSizeColumn(0); summary.setColumnWidth(1, 5500);

            Sheet byCat = wb.createSheet("By Category");
            byCat.setColumnWidth(0, 7000);
            writeReportHeader(byCat, "Stock by Category", dateRange, generatedBy, st);
            row = writeSectionTitle(byCat, 3, "Stock Value by Category", st);
            writeNameValueTable(byCat, row, new String[]{"Category", "Value (₹)"}, d.stockByCategory(), true, st);

            Sheet fast = wb.createSheet("Fast Moving");
            fast.setColumnWidth(0, 9000);
            writeReportHeader(fast, "Fast Moving Items", dateRange, generatedBy, st);
            row = writeSectionTitle(fast, 3, "Fast Moving Items", st);
            writeNameValueTable(fast, row, new String[]{"Item", "Units Consumed"}, d.fastMovingItems(), false, st);

            Sheet slow = wb.createSheet("Slow Moving");
            slow.setColumnWidth(0, 9000);
            writeReportHeader(slow, "Slow Moving Items", dateRange, generatedBy, st);
            row = writeSectionTitle(slow, 3, "Slow Moving Items", st);
            writeNameValueTable(slow, row, new String[]{"Item", "Units Consumed"}, d.slowMovingItems(), false, st);

            if (d.lowStockList() != null && !d.lowStockList().isEmpty()) {
                Sheet lowStock = wb.createSheet("Low Stock Alerts");
                lowStock.setColumnWidth(0, 9000); lowStock.setColumnWidth(1, 6000);
                lowStock.setColumnWidth(2, 4500); lowStock.setColumnWidth(3, 4500);
                writeReportHeader(lowStock, "Low Stock Alerts", dateRange, generatedBy, st);
                row = writeSectionTitle(lowStock, 3, "Low Stock Alert List", st);
                Row lhdr = lowStock.createRow(row++); lhdr.setHeightInPoints(15);
                for (int i = 0; i < 4; i++) {
                    Cell c = lhdr.createCell(i); c.setCellStyle(st.colHeader);
                    c.setCellValue(new String[]{"Item", "Category", "Current Stock", "Reorder Level"}[i]);
                }
                boolean a2 = false;
                for (InventoryAnalyticsResponse.LowStockEntry e : d.lowStockList()) {
                    Row r = lowStock.createRow(row++); r.setHeightInPoints(14);
                    r.createCell(0).setCellValue(e.itemName());       r.getCell(0).setCellStyle(a2 ? st.dataOdd : st.dataEven);
                    r.createCell(1).setCellValue(e.category());       r.getCell(1).setCellStyle(a2 ? st.dataOdd : st.dataEven);
                    r.createCell(2).setCellValue(e.currentStock());   r.getCell(2).setCellStyle(a2 ? st.numOdd  : st.numEven);
                    r.createCell(3).setCellValue(e.reorderLevel());   r.getCell(3).setCellStyle(a2 ? st.numOdd  : st.numEven);
                    a2 = !a2;
                }
            }

            Sheet trend = wb.createSheet("Stock Value Trend");
            trend.setColumnWidth(0, 4000);
            writeReportHeader(trend, "Stock Value Trend", dateRange, generatedBy, st);
            row = writeSectionTitle(trend, 3, "Stock Value Trend", st);
            writeTrendTable(trend, row, new String[]{"Date", "Value (₹)"}, d.stockValueTrend(), true, st);

            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Inventory Excel", e);
        }
    }

    // ── Generic fallback (kept for compatibility) ─────────────────────────
    public static byte[] build(String title, List<String> headers,
                               List<List<Object>> rows, String generatedBy) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles st = new Styles(wb);
            Sheet sheet = wb.createSheet("Report");
            sheet.setColumnWidth(0, 8000);
            writeReportHeader(sheet, title, "", generatedBy, st);

            Row hdrRow = sheet.createRow(3);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = hdrRow.createCell(i); c.setCellValue(headers.get(i)); c.setCellStyle(st.colHeader);
            }
            boolean alt = false;
            int rowNum = 4;
            for (List<Object> rowData : rows) {
                Row r = sheet.createRow(rowNum++);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell c = r.createCell(i);
                    Object v = rowData.get(i);
                    if (v instanceof Number n) { c.setCellValue(n.doubleValue()); c.setCellStyle(alt ? st.numOdd : st.numEven); }
                    else { c.setCellValue(v != null ? v.toString() : ""); c.setCellStyle(alt ? st.dataOdd : st.dataEven); }
                }
                alt = !alt;
            }
            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // helper record
    private record KpiEntry(String label, Double numericValue, String textValue, boolean isCurrency) {
        KpiEntry(String label, double v, boolean currency) { this(label, v, null, currency); }
        KpiEntry(String label, String text) { this(label, null, text, false); }
    }
}
