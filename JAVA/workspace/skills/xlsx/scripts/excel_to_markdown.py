#!/usr/bin/env python3
"""
Convert Excel file to Markdown format for easy reading and analysis.
Handles Chinese (Unicode) paths correctly.

Usage:
    python excel_to_markdown.py <excel_file_path> [output_file]

Example:
    python excel_to_markdown.py "D:\work\秒如\报价方案.xlsx"
    python excel_to_markdown.py "D:\work\秒如\报价方案.xlsx" "report.md"
"""

import pandas as pd
import sys
import os
import warnings
warnings.filterwarnings('ignore')

def excel_to_markdown(file_path, output_file='excel_summary.md'):
    """
    Convert Excel file to Markdown format.
    
    Args:
        file_path: Path to Excel file (can contain Chinese characters)
        output_file: Output markdown file path (default: excel_summary.md)
    """
    sys.stdout.reconfigure(encoding='utf-8')
    
    try:
        if not os.path.exists(file_path):
            print(f"❌ Error: File not found: {file_path}")
            sys.exit(1)
        
        print(f"📖 Reading: {file_path}")
        all_sheets = pd.read_excel(file_path, sheet_name=None)
        
        with open(output_file, 'w', encoding='utf-8') as md:
            # Document header
            md.write(f"# Excel File Analysis\n\n")
            md.write(f"**Source:** `{file_path}`\n\n")
            md.write(f"**Total Sheets:** {len(all_sheets)}\n\n")
            md.write("---\n\n")
            
            # Table of contents
            md.write("## Table of Contents\n\n")
            for idx, sheet_name in enumerate(all_sheets.keys(), 1):
                md.write(f"{idx}. [{sheet_name}](#sheet-{idx})\n")
            md.write("\n---\n\n")
            
            # Each sheet
            for idx, (sheet_name, df) in enumerate(all_sheets.items(), 1):
                max_row, max_col = df.shape
                
                md.write(f"## Sheet {idx}: {sheet_name}\n\n")
                md.write(f"**Dimensions:** {max_row} rows × {max_col} columns\n\n")
                
                # Column info
                md.write("### Columns\n\n")
                md.write("| Column | Type | Non-Null Count | Sample Values |\n")
                md.write("|--------|------|----------------|---------------|\n")
                
                for col in df.columns:
                    dtype = str(df[col].dtype)
                    non_null = df[col].count()
                    samples = df[col].dropna().head(3).tolist()
                    sample_str = ', '.join(str(s) for s in samples) if samples else 'N/A'
                    if len(sample_str) > 50:
                        sample_str = sample_str[:47] + '...'
                    md.write(f"| {col} | {dtype} | {non_null} | {sample_str} |\n")
                
                md.write("\n")
                
                # Data preview (first 20 rows as markdown table)
                preview_df = df.head(20)
                if not preview_df.empty:
                    md.write("### Data Preview (First 20 Rows)\n\n")
                    md.write(preview_df.to_markdown(index=False))
                    md.write("\n\n")
                
                # Statistics for numeric columns
                numeric_df = df.select_dtypes(include=['number'])
                if not numeric_df.empty:
                    md.write("### Numeric Statistics\n\n")
                    md.write(numeric_df.describe().to_markdown())
                    md.write("\n\n")
                
                md.write("---\n\n")
        
        print(f"✅ Markdown saved to: {output_file}")
        print(f"📊 Processed {len(all_sheets)} sheet(s)")
        return True
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    
    excel_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else 'excel_summary.md'
    
    excel_to_markdown(excel_path, output_path)
