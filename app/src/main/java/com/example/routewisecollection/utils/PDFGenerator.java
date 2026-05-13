package com.example.routewisecollection.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import com.example.routewisecollection.models.Transaction;
import com.example.routewisecollection.models.CustomerReportModel;
import com.example.routewisecollection.models.ReportData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PDFGenerator {
    
    private static final int PAGE_WIDTH = 595; // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 50;
    private static final int LINE_HEIGHT = 20;
    
    // Paint objects for text styling
    private static Paint titlePaint, headerPaint, normalPaint, smallPaint;
    
    // Daily Report PDF Generation
    public static String generateDailyReportPDF(Context context, List<Transaction> transactions, 
                                               String selectedDate, double totalCollection, 
                                               String agentName, String agentMobile, String companyName) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            setupPaintObjects();
            int yPosition = drawHeader(canvas, companyName, "Daily Collection Report", agentName, agentMobile);
            
            // Report specific info
            canvas.drawText("Report Date: " + selectedDate, MARGIN, yPosition, normalPaint);
            yPosition += 30;
            
            // Summary Section with background
            Paint summaryBgPaint = new Paint();
            summaryBgPaint.setColor(Color.rgb(232, 245, 233)); // Light green background
            summaryBgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN, yPosition + 60, summaryBgPaint);
            
            // Summary border
            Paint summaryBorderPaint = new Paint();
            summaryBorderPaint.setColor(Color.rgb(76, 175, 80)); // Green border
            summaryBorderPaint.setStyle(Paint.Style.STROKE);
            summaryBorderPaint.setStrokeWidth(2);
            canvas.drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN, yPosition + 60, summaryBorderPaint);
            
            canvas.drawText("📊 SUMMARY", MARGIN + 10, yPosition + 5, headerPaint);
            yPosition += 30;
            canvas.drawText("Total Entries: " + transactions.size(), MARGIN + 10, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            
            // Highlight total collection
            Paint totalPaint = new Paint();
            totalPaint.setColor(Color.rgb(27, 94, 32)); // Dark green
            totalPaint.setTextSize(14);
            totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            totalPaint.setAntiAlias(true);
            canvas.drawText("Total Collection: ₹" + String.format(Locale.getDefault(), "%.2f", totalCollection), MARGIN + 10, yPosition, totalPaint);
            yPosition += 50;
            
            // Transaction Details Section
            canvas.drawText("📋 TRANSACTION DETAILS", MARGIN, yPosition, headerPaint);
            yPosition += 30;
            
            // Table Header Background
            Paint headerBgPaint = new Paint();
            headerBgPaint.setColor(Color.rgb(227, 242, 253)); // Light blue background
            headerBgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(MARGIN, yPosition - 15, PAGE_WIDTH - MARGIN, yPosition + 10, headerBgPaint);
            
            // Table Headers with bold paint
            Paint tableHeaderPaint = new Paint();
            tableHeaderPaint.setColor(Color.BLACK);
            tableHeaderPaint.setTextSize(11);
            tableHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            tableHeaderPaint.setAntiAlias(true);
            
            canvas.drawText("Time", MARGIN + 5, yPosition, tableHeaderPaint);
            canvas.drawText("Customer", MARGIN + 80, yPosition, tableHeaderPaint);
            canvas.drawText("Type", MARGIN + 200, yPosition, tableHeaderPaint);
            canvas.drawText("Amount", MARGIN + 280, yPosition, tableHeaderPaint);
            canvas.drawText("Method", MARGIN + 360, yPosition, tableHeaderPaint);
            yPosition += 15;
            
            // Thick line under headers
            Paint thickLinePaint = new Paint();
            thickLinePaint.setColor(Color.rgb(25, 118, 210));
            thickLinePaint.setStrokeWidth(2);
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, thickLinePaint);
            yPosition += 15;
            
            // Alternating row colors
            Paint rowBgPaint = new Paint();
            rowBgPaint.setStyle(Paint.Style.FILL);
            
            int rowIndex = 0;
            for (Transaction transaction : transactions) {
                if (yPosition > PAGE_HEIGHT - 100) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = MARGIN + 30;
                    rowIndex = 0;
                }
                
                // Alternating row background
                if (rowIndex % 2 == 0) {
                    rowBgPaint.setColor(Color.rgb(250, 250, 250)); // Very light gray
                    canvas.drawRect(MARGIN, yPosition - 12, PAGE_WIDTH - MARGIN, yPosition + 6, rowBgPaint);
                }
                
                canvas.drawText(transaction.getTime() != null ? transaction.getTime() : "N/A", MARGIN + 5, yPosition, smallPaint);
                canvas.drawText(transaction.getCustomerName() != null ? transaction.getCustomerName() : "Unknown", MARGIN + 80, yPosition, smallPaint);
                canvas.drawText(transaction.getTypeDisplay(), MARGIN + 200, yPosition, smallPaint);
                
                // Amount in bold
                Paint amountPaint = new Paint();
                amountPaint.setColor(Color.rgb(27, 94, 32)); // Dark green
                amountPaint.setTextSize(10);
                amountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                amountPaint.setAntiAlias(true);
                canvas.drawText("₹" + String.format(Locale.getDefault(), "%.2f", transaction.getAmount()), MARGIN + 280, yPosition, amountPaint);
                
                String methodDisplay = transaction.getMode() != null ? transaction.getMode().toUpperCase() : "CASH";
                if ("ONLINE".equalsIgnoreCase(transaction.getMode()) && transaction.getOnlineMobileNumber() != null && !transaction.getOnlineMobileNumber().isEmpty()) {
                    methodDisplay += " - " + transaction.getOnlineMobileNumber();
                }
                canvas.drawText(methodDisplay, MARGIN + 360, yPosition, smallPaint);
                yPosition += 18;
                rowIndex++;
            }
            
            return finalizePDF(pdfDocument, page, canvas, "Daily_Report_" + selectedDate.replace("-", "_"));
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // Weekly Report PDF Generation
    public static String generateWeeklyReportPDF(Context context, ReportData.WeeklyReport weeklyReport,
                                                 String weekPeriod, String agentName, String agentMobile, String companyName, String companyPhone) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            setupPaintObjects();
            int yPosition = drawHeader(canvas, companyName, "Weekly Collection Report", agentName, agentMobile);
            
            // Report specific info
            canvas.drawText("Week Period: " + weekPeriod, MARGIN, yPosition, normalPaint);
            yPosition += 30;
            
            // Summary
            canvas.drawText("SUMMARY", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            canvas.drawText("Total Entries: " + weeklyReport.getTotalEntries(), MARGIN, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            canvas.drawText("Total Collection: ₹" + String.format(Locale.getDefault(), "%.2f", weeklyReport.getTotalCollection()), MARGIN, yPosition, normalPaint);
            yPosition += 40;
            
            // Daily Breakdown
            canvas.drawText("DAILY BREAKDOWN", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            
            // Table Headers
            canvas.drawText("Date", MARGIN, yPosition, normalPaint);
            canvas.drawText("Day", MARGIN + 120, yPosition, normalPaint);
            canvas.drawText("Entries", MARGIN + 200, yPosition, normalPaint);
            canvas.drawText("Collection", MARGIN + 280, yPosition, normalPaint);
            yPosition += 20;
            
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, normalPaint);
            yPosition += 15;
            
            for (ReportData.DailySummary daily : weeklyReport.getDailySummaries()) {
                if (yPosition > PAGE_HEIGHT - 100) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = MARGIN + 30;
                }
                
                canvas.drawText(daily.getDate(), MARGIN, yPosition, smallPaint);
                canvas.drawText(daily.getDayOfWeek() != null ? daily.getDayOfWeek() : "", MARGIN + 120, yPosition, smallPaint);
                canvas.drawText(String.valueOf(daily.getEntryCount()), MARGIN + 200, yPosition, smallPaint);
                canvas.drawText("₹" + String.format(Locale.getDefault(), "%.2f", daily.getTotalAmount()), MARGIN + 280, yPosition, smallPaint);
                yPosition += 18;
            }
            
            return finalizePDF(pdfDocument, page, canvas, "Weekly_Report_" + weekPeriod.replace(" ", "_"));
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // Monthly Report PDF Generation
    public static String generateMonthlyReportPDF(Context context, ReportData.MonthlyReport monthlyReport, 
                                                 String selectedMonth, String agentName, String agentMobile, String companyName) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            setupPaintObjects();
            int yPosition = drawHeader(canvas, companyName, "Monthly Collection Report", agentName, agentMobile);
            
            // Report specific info
            canvas.drawText("Report Month: " + selectedMonth, MARGIN, yPosition, normalPaint);
            yPosition += 30;
            
            // Summary
            canvas.drawText("SUMMARY", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            canvas.drawText("Total Days: " + monthlyReport.getDailySummaries().size(), MARGIN, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            canvas.drawText("Total Entries: " + monthlyReport.getTotalEntries(), MARGIN, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            canvas.drawText("Total Collection: ₹" + String.format(Locale.getDefault(), "%.2f", monthlyReport.getTotalCollection()), MARGIN, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            canvas.drawText("Total Customers: " + monthlyReport.getTotalCustomers(), MARGIN, yPosition, normalPaint);
            yPosition += 40;
            
            // Daily Breakdown
            canvas.drawText("DAILY BREAKDOWN", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            
            // Table Headers
            canvas.drawText("Date", MARGIN, yPosition, normalPaint);
            canvas.drawText("Entries", MARGIN + 120, yPosition, normalPaint);
            canvas.drawText("Collection", MARGIN + 200, yPosition, normalPaint);
            canvas.drawText("Customers", MARGIN + 300, yPosition, normalPaint);
            yPosition += 20;
            
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, normalPaint);
            yPosition += 15;
            
            for (ReportData.DailySummary daily : monthlyReport.getDailySummaries()) {
                if (yPosition > PAGE_HEIGHT - 100) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = MARGIN + 30;
                }
                
                canvas.drawText(daily.getDate(), MARGIN, yPosition, smallPaint);
                canvas.drawText(String.valueOf(daily.getEntryCount()), MARGIN + 120, yPosition, smallPaint);
                canvas.drawText("₹" + String.format(Locale.getDefault(), "%.2f", daily.getTotalAmount()), MARGIN + 200, yPosition, smallPaint);
                canvas.drawText(String.valueOf(daily.getCustomerCount()), MARGIN + 300, yPosition, smallPaint);
                yPosition += 18;
            }
            
            return finalizePDF(pdfDocument, page, canvas, "Monthly_Report_" + selectedMonth.replace(" ", "_"));
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // Customer Report PDF Generation
    public static String generateCustomerReportPDF(Context context, List<CustomerReportModel> customerReports, 
                                                  double totalAmount, String agentName, String agentMobile, String companyName) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            setupPaintObjects();
            int yPosition = drawHeader(canvas, companyName, "Customer Report", agentName, agentMobile);
            
            // Summary Section with background
            Paint summaryBgPaint = new Paint();
            summaryBgPaint.setColor(Color.rgb(232, 245, 233)); // Light green background
            summaryBgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN, yPosition + 60, summaryBgPaint);
            
            // Summary border
            Paint summaryBorderPaint = new Paint();
            summaryBorderPaint.setColor(Color.rgb(76, 175, 80)); // Green border
            summaryBorderPaint.setStyle(Paint.Style.STROKE);
            summaryBorderPaint.setStrokeWidth(2);
            canvas.drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN, yPosition + 60, summaryBorderPaint);
            
            canvas.drawText("📊 SUMMARY", MARGIN + 10, yPosition + 5, headerPaint);
            yPosition += 30;
            canvas.drawText("Total Customers: " + customerReports.size(), MARGIN + 10, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            
            // Highlight total amount
            Paint totalPaint = new Paint();
            totalPaint.setColor(Color.rgb(27, 94, 32)); // Dark green
            totalPaint.setTextSize(14);
            totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            totalPaint.setAntiAlias(true);
            canvas.drawText("Total Amount: ₹" + String.format(Locale.getDefault(), "%.2f", totalAmount), MARGIN + 10, yPosition, totalPaint);
            yPosition += 50;
            
            // Customer Details Section
            canvas.drawText("📋 CUSTOMER DETAILS", MARGIN, yPosition, headerPaint);
            yPosition += 30;
            
            // Table Header Background
            Paint headerBgPaint = new Paint();
            headerBgPaint.setColor(Color.rgb(227, 242, 253)); // Light blue background
            headerBgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(MARGIN, yPosition - 15, PAGE_WIDTH - MARGIN, yPosition + 10, headerBgPaint);
            
            // Table Headers with bold paint
            Paint tableHeaderPaint = new Paint();
            tableHeaderPaint.setColor(Color.BLACK);
            tableHeaderPaint.setTextSize(11);
            tableHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            tableHeaderPaint.setAntiAlias(true);
            
            canvas.drawText("Customer Name", MARGIN + 5, yPosition, tableHeaderPaint);
            canvas.drawText("Account No", MARGIN + 150, yPosition, tableHeaderPaint);
            canvas.drawText("Amount", MARGIN + 260, yPosition, tableHeaderPaint);
            canvas.drawText("Entries", MARGIN + 360, yPosition, tableHeaderPaint);
            yPosition += 15;
            
            // Thick line under headers
            Paint thickLinePaint = new Paint();
            thickLinePaint.setColor(Color.rgb(25, 118, 210));
            thickLinePaint.setStrokeWidth(2);
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, thickLinePaint);
            yPosition += 15;
            
            // Alternating row colors and bold text
            Paint rowBgPaint = new Paint();
            rowBgPaint.setStyle(Paint.Style.FILL);
            
            // Bold paint for customer data
            Paint boldDataPaint = new Paint();
            boldDataPaint.setColor(Color.BLACK);
            boldDataPaint.setTextSize(10);
            boldDataPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            boldDataPaint.setAntiAlias(true);
            
            // Amount paint - green and bold
            Paint amountPaint = new Paint();
            amountPaint.setColor(Color.rgb(27, 94, 32)); // Dark green
            amountPaint.setTextSize(11);
            amountPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            amountPaint.setAntiAlias(true);
            
            // Entries paint - blue and bold
            Paint entriesPaint = new Paint();
            entriesPaint.setColor(Color.rgb(25, 118, 210)); // Blue
            entriesPaint.setTextSize(10);
            entriesPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            entriesPaint.setAntiAlias(true);
            
            int rowIndex = 0;
            for (CustomerReportModel customer : customerReports) {
                if (yPosition > PAGE_HEIGHT - 100) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = MARGIN + 30;
                    rowIndex = 0;
                }
                
                // Alternating row background
                if (rowIndex % 2 == 0) {
                    rowBgPaint.setColor(Color.rgb(250, 250, 250)); // Very light gray
                    canvas.drawRect(MARGIN, yPosition - 12, PAGE_WIDTH - MARGIN, yPosition + 6, rowBgPaint);
                }
                
                // Draw table borders for each cell
                Paint cellBorderPaint = new Paint();
                cellBorderPaint.setColor(Color.rgb(224, 224, 224)); // Light gray
                cellBorderPaint.setStyle(Paint.Style.STROKE);
                cellBorderPaint.setStrokeWidth(1);
                
                // Customer Name - Bold
                canvas.drawText(customer.getCustomerName() != null ? customer.getCustomerName() : "N/A", 
                    MARGIN + 5, yPosition, boldDataPaint);
                
                // Account Number - Bold
                canvas.drawText(customer.getAccountNumber() != null ? customer.getAccountNumber() : "N/A", 
                    MARGIN + 150, yPosition, boldDataPaint);
                
                // Amount - Bold and Green
                canvas.drawText("₹" + String.format(Locale.getDefault(), "%.2f", customer.getTotalAmount()), 
                    MARGIN + 260, yPosition, amountPaint);
                
                // Entries - Bold and Blue
                canvas.drawText(String.valueOf(customer.getTotalEntries()), 
                    MARGIN + 360, yPosition, entriesPaint);
                
                yPosition += 18;
                rowIndex++;
            }
            
            return finalizePDF(pdfDocument, page, canvas, "Customer_Report");
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // Withdraw Report PDF Generation
    public static String generateWithdrawReportPDF(Context context, List<Transaction> withdrawals, 
                                                  String selectedMonth, double totalWithdrawals, 
                                                  String agentName, String agentMobile, String companyName) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            setupPaintObjects();
            int yPosition = drawHeader(canvas, companyName, "Withdraw Report", agentName, agentMobile);
            
            // Report specific info
            canvas.drawText("Report Period: " + selectedMonth, MARGIN, yPosition, normalPaint);
            yPosition += 30;
            
            // Summary
            canvas.drawText("SUMMARY", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            canvas.drawText("Total Withdrawals: " + withdrawals.size(), MARGIN, yPosition, normalPaint);
            yPosition += LINE_HEIGHT;
            canvas.drawText("Total Amount: ₹" + String.format(Locale.getDefault(), "%.2f", totalWithdrawals), MARGIN, yPosition, normalPaint);
            yPosition += 40;
            
            // Withdrawal Details
            canvas.drawText("WITHDRAWAL DETAILS", MARGIN, yPosition, headerPaint);
            yPosition += 25;
            
            // Table Headers
            canvas.drawText("Date", MARGIN, yPosition, normalPaint);
            canvas.drawText("Customer", MARGIN + 80, yPosition, normalPaint);
            canvas.drawText("Amount", MARGIN + 200, yPosition, normalPaint);
            canvas.drawText("Method", MARGIN + 280, yPosition, normalPaint);
            canvas.drawText("Time", MARGIN + 420, yPosition, normalPaint);
            yPosition += 20;
            
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, normalPaint);
            yPosition += 15;
            
            for (Transaction withdrawal : withdrawals) {
                if (yPosition > PAGE_HEIGHT - 100) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = MARGIN + 30;
                }
                
                canvas.drawText(withdrawal.getDate() != null ? withdrawal.getDate() : "N/A", MARGIN, yPosition, smallPaint);
                canvas.drawText(withdrawal.getCustomerName() != null ? withdrawal.getCustomerName() : "Unknown", MARGIN + 80, yPosition, smallPaint);
                canvas.drawText("₹" + String.format(Locale.getDefault(), "%.2f", withdrawal.getAmount()), MARGIN + 200, yPosition, smallPaint);
                
                String methodDisplay = withdrawal.getMode() != null ? withdrawal.getMode().toUpperCase() : "CASH";
                if ("ONLINE".equalsIgnoreCase(withdrawal.getMode()) && withdrawal.getOnlineMobileNumber() != null && !withdrawal.getOnlineMobileNumber().isEmpty()) {
                    methodDisplay += " - " + withdrawal.getOnlineMobileNumber();
                }
                canvas.drawText(methodDisplay, MARGIN + 280, yPosition, smallPaint);
                
                canvas.drawText(withdrawal.getTime() != null ? withdrawal.getTime() : "N/A", MARGIN + 420, yPosition, smallPaint);
                yPosition += 18;
            }
            
            return finalizePDF(pdfDocument, page, canvas, "Withdraw_Report_" + selectedMonth.replace(" ", "_"));
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    // Helper methods
    private static void setupPaintObjects() {
        // Title Paint - Large, Bold, Dark Blue
        titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(25, 118, 210)); // Material Blue
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(true);
        
        // Header Paint - Medium, Bold, Dark Gray
        headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(66, 66, 66)); // Dark Gray
        headerPaint.setTextSize(16);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerPaint.setAntiAlias(true);
        
        // Normal Paint - Regular, Black
        normalPaint = new Paint();
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(12);
        normalPaint.setAntiAlias(true);
        
        // Small Paint - Small, Gray
        smallPaint = new Paint();
        smallPaint.setColor(Color.rgb(117, 117, 117)); // Medium Gray
        smallPaint.setTextSize(10);
        smallPaint.setAntiAlias(true);
    }
    
    private static int drawHeader(Canvas canvas, String companyName, String reportTitle, String agentName, String agentMobile) {
        // Draw complete page outline/border
        Paint pageOutlinePaint = new Paint();
        pageOutlinePaint.setColor(Color.rgb(25, 118, 210)); // Material Blue
        pageOutlinePaint.setStyle(Paint.Style.STROKE);
        pageOutlinePaint.setStrokeWidth(4);
        canvas.drawRect(MARGIN - 10, MARGIN - 10, PAGE_WIDTH - MARGIN + 10, PAGE_HEIGHT - MARGIN + 10, pageOutlinePaint);
        
        int yPosition = MARGIN + 20;
        
        // Draw top decorative border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(25, 118, 210)); // Material Blue
        borderPaint.setStrokeWidth(3);
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, borderPaint);
        yPosition += 25;
        
        // Company Name with icon - Larger and more prominent with text wrapping
        Paint companyPaint = new Paint();
        companyPaint.setColor(Color.rgb(25, 118, 210)); // Material Blue
        companyPaint.setTextSize(22); // Slightly reduced from 26
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        companyPaint.setAntiAlias(true);
        
        // Calculate available width for company name
        int availableWidth = PAGE_WIDTH - (2 * MARGIN) - 30; // 30 for icon and padding
        String companyText = "🏢 " + companyName;
        
        // Measure text width
        float textWidth = companyPaint.measureText(companyText);
        
        if (textWidth > availableWidth) {
            // Text is too long - need to wrap or reduce size
            // Try smaller font first
            companyPaint.setTextSize(18);
            textWidth = companyPaint.measureText(companyText);
            
            if (textWidth > availableWidth) {
                // Still too long - split into multiple lines
                companyPaint.setTextSize(20); // Use medium size
                
                // Split company name into words
                String[] words = companyName.split(" ");
                StringBuilder line1 = new StringBuilder("🏢 ");
                StringBuilder line2 = new StringBuilder();
                
                for (String word : words) {
                    String testLine = line1.toString() + word + " ";
                    if (companyPaint.measureText(testLine) < availableWidth) {
                        line1.append(word).append(" ");
                    } else {
                        line2.append(word).append(" ");
                    }
                }
                
                // Draw first line
                canvas.drawText(line1.toString().trim(), MARGIN, yPosition, companyPaint);
                yPosition += 28;
                
                // Draw second line if exists
                if (line2.length() > 0) {
                    canvas.drawText(line2.toString().trim(), MARGIN + 25, yPosition, companyPaint);
                    yPosition += 28;
                }
            } else {
                // Smaller font fits in one line
                canvas.drawText(companyText, MARGIN, yPosition, companyPaint);
                yPosition += 30;
            }
        } else {
            // Normal size fits fine
            canvas.drawText(companyText, MARGIN, yPosition, companyPaint);
            yPosition += 35;
        }
        
        // Report Title with underline
        canvas.drawText(reportTitle, MARGIN, yPosition, headerPaint);
        
        // Underline for title
        Paint underlinePaint = new Paint();
        underlinePaint.setColor(Color.rgb(25, 118, 210));
        underlinePaint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, yPosition + 5, MARGIN + headerPaint.measureText(reportTitle), yPosition + 5, underlinePaint);
        yPosition += 30;
        
        // Info box background with gradient effect
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.rgb(240, 248, 255)); // Alice blue background
        boxPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, yPosition - 15, PAGE_WIDTH - MARGIN, yPosition + 55, boxPaint);
        
        // Info box border - thicker
        Paint boxBorderPaint = new Paint();
        boxBorderPaint.setColor(Color.rgb(25, 118, 210)); // Blue border
        boxBorderPaint.setStyle(Paint.Style.STROKE);
        boxBorderPaint.setStrokeWidth(2);
        canvas.drawRect(MARGIN, yPosition - 15, PAGE_WIDTH - MARGIN, yPosition + 55, boxBorderPaint);
        
        // Generated date with icon
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("📅 Generated: " + currentDate, MARGIN + 10, yPosition, normalPaint);
        yPosition += LINE_HEIGHT;
        
        // Agent name with icon - More prominent
        Paint agentPaint = new Paint();
        agentPaint.setColor(Color.rgb(27, 94, 32)); // Dark green
        agentPaint.setTextSize(13);
        agentPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        agentPaint.setAntiAlias(true);
        canvas.drawText("👤 Agent: " + agentName, MARGIN + 10, yPosition, agentPaint);
        yPosition += LINE_HEIGHT;
        
        // Agent mobile number - Actual contact
        Paint mobilePaint = new Paint();
        mobilePaint.setColor(Color.rgb(25, 118, 210)); // Blue
        mobilePaint.setTextSize(12);
        mobilePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mobilePaint.setAntiAlias(true);
        String contactText = (agentMobile != null && !agentMobile.isEmpty()) 
            ? "📱 Contact: " + agentMobile 
            : "📱 Contact: Not Available";
        canvas.drawText(contactText, MARGIN + 10, yPosition, mobilePaint);
        yPosition += 35;
        
        // Bottom decorative border
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, borderPaint);
        yPosition += 25;
        
        return yPosition;
    }
    
    private static String finalizePDF(PdfDocument pdfDocument, PdfDocument.Page page, Canvas canvas, String filePrefix) throws IOException {
        // Footer with decorative border
        int yPosition = PAGE_HEIGHT - 70;
        
        // Top border for footer - thicker
        Paint footerBorderPaint = new Paint();
        footerBorderPaint.setColor(Color.rgb(25, 118, 210)); // Blue
        footerBorderPaint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, footerBorderPaint);
        yPosition += 15;
        
        // Footer background
        Paint footerBgPaint = new Paint();
        footerBgPaint.setColor(Color.rgb(240, 248, 255)); // Alice blue
        footerBgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition + 30, footerBgPaint);
        
        // Footer border
        Paint footerBoxBorderPaint = new Paint();
        footerBoxBorderPaint.setColor(Color.rgb(25, 118, 210));
        footerBoxBorderPaint.setStyle(Paint.Style.STROKE);
        footerBoxBorderPaint.setStrokeWidth(1);
        canvas.drawRect(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN, yPosition + 30, footerBoxBorderPaint);
        
        // Footer text - left side
        Paint footerTextPaint = new Paint();
        footerTextPaint.setColor(Color.rgb(66, 66, 66));
        footerTextPaint.setTextSize(10);
        footerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        footerTextPaint.setAntiAlias(true);
        canvas.drawText("📱 Generated by SmartBishi App", MARGIN + 10, yPosition + 15, footerTextPaint);
        
        // Footer text - right side (page number)
        canvas.drawText("Page 1", PAGE_WIDTH - MARGIN - 60, yPosition + 15, footerTextPaint);
        
        // Watermark text (optional)
        Paint watermarkPaint = new Paint();
        watermarkPaint.setColor(Color.rgb(200, 200, 200));
        watermarkPaint.setTextSize(8);
        watermarkPaint.setAntiAlias(true);
        canvas.drawText("This is a computer generated report", MARGIN + 10, yPosition + 28, watermarkPaint);
        
        pdfDocument.finishPage(page);
        
        // Save PDF
        String fileName = filePrefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pdfFile = new File(downloadsDir, fileName);
        
        FileOutputStream fos = new FileOutputStream(pdfFile);
        pdfDocument.writeTo(fos);
        pdfDocument.close();
        fos.close();
        
        return pdfFile.getAbsolutePath();
    }
}