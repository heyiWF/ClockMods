/** Allocate only the space left by the primary clock to supporting text and gaps. */
export function fitSupportingRows(height: number, primaryHeight: number, dateSize: number,
  supportingSize: number, dateRows: number, supportingRows: number, portrait: boolean) {
  const timeGap = Math.min(height * .025, dateSize * (portrait ? .9 : .35));
  const rowGap = Math.min(height * .015, dateSize * (portrait ? .5 : .35));
  const gapCount = Number(dateRows > 0) + Number(supportingRows > 0);
  const rowGapCount = Math.max(0, dateRows - 1) + Math.max(0, supportingRows - 1);
  const requested = (dateSize * dateRows + supportingSize * supportingRows) * 1.2
    + timeGap * gapCount + rowGap * rowGapCount;
  const scale = requested > 0 ? Math.min(1, Math.max(0, height - primaryHeight) / requested) : 1;
  return { dateSize: dateSize * scale, supportingSize: supportingSize * scale,
    timeGap: timeGap * scale, rowGap: rowGap * scale };
}
