/** Each row owns an equal share of the remaining vertical space; siblings never rescale it. */
export function fitSupportingRows(height:number,primaryHeight:number,dateSize:number,supportingSize:number,dateRows:number,supportingRows:number,_portrait:boolean){
 const count=dateRows+supportingRows,available=Math.max(0,height-primaryHeight);
 const timeGap=Math.min(height*.015,available*.06),rowGap=Math.min(height*.01,available*.04);
 const gaps=timeGap*(Number(dateRows>0)+Number(supportingRows>0))+rowGap*(Math.max(0,dateRows-1)+Math.max(0,supportingRows-1));
 const cap=count?Math.max(0,available-gaps)/count/1.2:Infinity;
 return {dateSize:Math.min(dateSize,cap),supportingSize:Math.min(supportingSize,cap),timeGap,rowGap};
}
