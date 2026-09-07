export type SpreadsheetCell = string | number | boolean | null | undefined;

export function spreadsheetFilename(kind: string) {
  return `spectra-recent-${kind}-${new Date().toISOString().slice(0, 10)}.xls`;
}

function xmlEscape(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}

function cellXml(value: SpreadsheetCell) {
  if (value === null || value === undefined || value === "") {
    return "<Cell/>";
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return `<Cell><Data ss:Type="Number">${value}</Data></Cell>`;
  }
  if (typeof value === "boolean") {
    return `<Cell><Data ss:Type="Boolean">${value ? 1 : 0}</Data></Cell>`;
  }
  return `<Cell><Data ss:Type="String">${xmlEscape(String(value))}</Data></Cell>`;
}

function rowXml(cells: SpreadsheetCell[]) {
  return `<Row>${cells.map(cellXml).join("")}</Row>`;
}

export function downloadSpreadsheet(filename: string, headers: string[], rows: SpreadsheetCell[][]) {
  const table = [rowXml(headers), ...rows.map(rowXml)].join("");
  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
<Worksheet ss:Name="Export"><Table>${table}</Table></Worksheet>
</Workbook>`;
  const blob = new Blob([xml], { type: "application/vnd.ms-excel" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename.endsWith(".xls") ? filename : `${filename}.xls`;
  a.click();
  URL.revokeObjectURL(url);
}
