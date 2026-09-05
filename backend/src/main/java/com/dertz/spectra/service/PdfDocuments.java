package com.dertz.spectra.service;

import com.dertz.spectra.dto.StaffPayslip;
import com.dertz.spectra.model.PurchaseOrder;
import com.dertz.spectra.model.PurchaseOrderItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfDocuments {

	private static final Color MOSS = new Color(31, 107, 74);
	private static final Color INK = new Color(20, 34, 28);
	private static final Color MUTED = new Color(90, 102, 96);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of("Asia/Kolkata"));

	public byte[] payslip(StaffPayslip slip, String org, String centre) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
		PdfWriter.getInstance(doc, out);
		doc.open();
		banner(doc, org, centre);
		title(doc, "Payslip");
		muted(doc, slip.getPeriodLabel() + (slip.isLocked() ? " · " + pretty(slip.getRunStatus()) : " · provisional"));
		kv(doc, new String[][] {
				{ "Staff", nullToDash(slip.getFullName()) },
				{ "Role", pretty(slip.getRole()) },
				{ "Email", nullToDash(slip.getEmail()) },
				{ "Base salary", rs(slip.getBaseSalary()) },
				{ "Working days", String.valueOf(slip.getWorkingDays()) },
				{ "Daily wage", rs(slip.getDailyWage()) },
				{ "Unpaid / LOP days", n(slip.getUnpaidLeaveDays()) },
				{ "LOP deduction", rs(slip.getLopDeduction()) },
				{ "Store dues", rs(slip.getStoreDues()) },
				{ "Advance", rs(slip.getAdvanceDeduction()) },
				{ "Other deductions", rs(slip.getOtherDeductions()) },
				{ "Pocket reimbursements", rs(slip.getReimbursementCredit()) },
				{ "Rolled-over store debt", rs(slip.getRolledOverStoreDebt()) },
				{ "Net payout", rs(slip.getNetPayout()) },
		});
		if (!slip.isLocked()) {
			muted(doc, "This is a live estimate for the current month. Figures lock when payroll is generated and approved.");
		}
		doc.close();
		return out.toByteArray();
	}

	public byte[] purchaseOrder(PurchaseOrder po, String org, String centre) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
		PdfWriter.getInstance(doc, out);
		doc.open();
		banner(doc, org, centre);
		title(doc, "Purchase order " + po.getPoNumber());
		muted(doc, pretty(po.getStatus() == null ? "" : po.getStatus().name()));
		kv(doc, new String[][] {
				{ "Supplier", nullToDash(po.getSupplierName()) },
				{ "Raised by", nullToDash(po.getRequestedByName()) },
				{ "Approver", nullToDash(po.getApprovedByName() != null ? po.getApprovedByName() : po.getApproverName()) },
				{ "Submitted", po.getSubmittedAt() == null ? "—" : DAY.format(po.getSubmittedAt()) },
				{ "Approved", po.getApprovedAt() == null ? "—" : DAY.format(po.getApprovedAt()) },
				{ "Expected total", rs(po.getExpectedTotal()) },
				{ "Received total", rs(po.getReceivedTotal()) },
				{ "Notes", nullToDash(po.getNotes()) },
		});
		List<PurchaseOrderItem> lines = po.getLines() == null ? List.of() : po.getLines();
		PdfPTable table = new PdfPTable(new float[] { 4.2f, 1.2f, 1.4f, 1.4f, 1.4f });
		table.setWidthPercentage(100);
		table.setSpacingBefore(12);
		headerCell(table, "Item");
		headerCell(table, "Ordered");
		headerCell(table, "Unit");
		headerCell(table, "Received");
		headerCell(table, "Line");
		for (PurchaseOrderItem line : lines) {
			String name = line.getProductName() == null ? "Product " + line.getShelterProductId() : line.getProductName();
			if (line.getSku() != null && !line.getSku().isBlank()) {
				name = name + " (" + line.getSku() + ")";
			}
			bodyCell(table, name, Element.ALIGN_LEFT);
			bodyCell(table, n(line.getQtyOrdered()), Element.ALIGN_RIGHT);
			bodyCell(table, rs(line.getUnitCost()), Element.ALIGN_RIGHT);
			bodyCell(table, n(line.getQtyReceived()), Element.ALIGN_RIGHT);
			BigDecimal lineTotal = nz(line.getQtyOrdered()).multiply(nz(line.getUnitCost())).setScale(2, RoundingMode.HALF_UP);
			bodyCell(table, rs(lineTotal), Element.ALIGN_RIGHT);
		}
		doc.add(table);
		if (po.getRejectReason() != null && !po.getRejectReason().isBlank()) {
			muted(doc, "Rejected: " + po.getRejectReason());
		}
		doc.close();
		return out.toByteArray();
	}

	private void banner(Document doc, String org, String centre) {
		Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, MOSS);
		Font sub = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
		doc.add(new Paragraph("Spectra NGO", brand));
		doc.add(new Paragraph(nullToDash(org) + " · " + nullToDash(centre), sub));
	}

	private void title(Document doc, String text) {
		Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INK);
		Paragraph p = new Paragraph(text, font);
		p.setSpacingBefore(14);
		p.setSpacingAfter(4);
		doc.add(p);
	}

	private void muted(Document doc, String text) {
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
		Paragraph p = new Paragraph(text, font);
		p.setSpacingAfter(8);
		doc.add(p);
	}

	private void kv(Document doc, String[][] rows) {
		PdfPTable table = new PdfPTable(new float[] { 1.4f, 2.6f });
		table.setWidthPercentage(100);
		table.setSpacingBefore(6);
		for (String[] row : rows) {
			labelCell(table, row[0]);
			bodyCell(table, row[1], Element.ALIGN_LEFT);
		}
		doc.add(table);
	}

	private void headerCell(PdfPTable table, String text) {
		Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBackgroundColor(MOSS);
		cell.setPadding(6);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		table.addCell(cell);
	}

	private void labelCell(PdfPTable table, String text) {
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBorderColor(new Color(220, 226, 222));
		cell.setPadding(6);
		table.addCell(cell);
	}

	private void bodyCell(PdfPTable table, String text, int align) {
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, INK);
		PdfPCell cell = new PdfPCell(new Phrase(text == null ? "—" : text, font));
		cell.setBorderColor(new Color(220, 226, 222));
		cell.setPadding(6);
		cell.setHorizontalAlignment(align);
		table.addCell(cell);
	}

	private static String rs(BigDecimal value) {
		return "Rs " + nz(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String n(BigDecimal value) {
		return nz(value).stripTrailingZeros().toPlainString();
	}

	private static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static String pretty(String value) {
		if (value == null || value.isBlank()) {
			return "—";
		}
		return value.replace('_', ' ');
	}

	private static String nullToDash(String value) {
		return value == null || value.isBlank() ? "—" : value;
	}
}
