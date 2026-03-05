#!/usr/bin/env python3
"""
Read Excel files with Chinese (Unicode) paths and content.
Saves formatted output to a text file for easy viewing.

Usage:
    python read_chinese_excel.py <excel_file_path> [output_file]

Example:
    python read_chinese_excel.py "D:\work\秒如\报价方案.xlsx"
    python read_chinese_excel.py "D:\work\秒如\报价方案.xlsx" "output.txt"
"""

import pandas as pd
import sys
import os
import warnings
warnings.filterwarnings('ignore')

def read_excel_with_chinese_path(file_path, output_file='excel_content.txt'):
    """
    Read Excel file with Chinese path and save formatted content to text file.
    
    Args:
        file_path: Path to Excel file (can contain Chinese characters)
        output_file: Output text file path (default: excel_content.txt)
    """
    # Configure output encoding for Chinese characters
    sys.stdout.reconfigure(encoding='utf-8')
    
    try:
        # Verify file exists
        if not os.path.exists(file_path):
            print(f"❌ Error: File not found: {file_path}")
            sys.exit(1)
        
        # Read all sheets
        print(f"📖 Reading: {file_path}")
        all_sheets = pd.read_excel(file_path, sheet_name=None)
        
        with open(output_file, 'w', encoding='utf-8') as out:
            # Header
            out.write("=" * 70 + "\n")
            out.write("Excel File Content Summary\n")
            out.write("=" * 70 + "\n")
            out.write(f"File: {file_path}\n")
            out.write(f"Total Sheets: {len(all_sheets)}\n")
            out.write(f"Sheet Names: {', '.join(all_sheets.keys())}\n")
            out.write("=" * 70 + "\n\n")
            
            # Process each sheet
            for idx, (sheet_name, df) in enumerate(all_sheets.items(), 1):
                out.write(f"\n【Sheet {idx}: {sheet_name}】\n")
                out.write("-" * 50 + "\n")
                
                # Sheet info
                max_row, max_col = df.shape
                out.write(f"Dimensions: {max_row} rows × {max_col} columns\n")
                out.write(f"Columns: {list(df.columns)}\n\n")
                
                # Data preview (first 50 rows)
                preview_rows = min(50, max_row)
                out.write(f"First {preview_rows} rows:\n")
                out.write(df.head(preview_rows).to_string())
                out.write("\n\n")
                
                # Show summary statistics for numeric columns
                numeric_cols = df.select_dtypes(include=['number']).columns
                if len(numeric_cols) > 0:
                    out.write("Numeric Column Statistics:\n")
                    out.write(df[numeric_cols].describe().to_string())
                    out.write("\n\n")
        
        print(f"✅ Successfully saved to: {output_file}")
        print(f"📊 Total sheets: {len(all_sheets)}")
        for name in all_sheets.keys():
            shape = all_sheets[name].shape
            print(f"   • {name}: {shape[0]} rows × {shape[1]} cols")
        
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
    output_path = sys.argv[2] if len(sys.argv) > 2 else 'excel_content.txt'
    
    read_excel_with_chinese_path(excel_path, output_path)
