#!/usr/bin/env node
/**
 * Read file with Chinese (Unicode) path support.
 * Usage: node read_chinese_file.js <filepath>
 */

const fs = require('fs').promises;
const path = require('path');

/**
 * Read a file with Chinese characters in path.
 * @param {string} filepath - Path to file (may contain Chinese characters)
 * @returns {Promise<string>} File content as string
 */
async function readChineseFile(filepath) {
    try {
        // Node.js uses UTF-8 by default
        const content = await fs.readFile(filepath, 'utf-8');
        return content;
    } catch (error) {
        if (error.code === 'ENOENT') {
            throw new Error(`File not found: ${filepath}`);
        }
        throw error;
    }
}

/**
 * Traverse directory containing Chinese filenames.
 * @param {string} directory - Root directory path
 * @param {string} pattern - File pattern to match (e.g., '*.txt')
 * @returns {Promise<Array>} List of file paths
 */
async function traverseChineseDir(directory, pattern = '*') {
    const results = [];
    
    async function traverse(currentDir) {
        const entries = await fs.readdir(currentDir, { withFileTypes: true });
        
        for (const entry of entries) {
            const fullPath = path.join(currentDir, entry.name);
            
            if (entry.isDirectory()) {
                await traverse(fullPath);
            } else {
                // Simple pattern matching (supports * wildcard)
                if (pattern === '*' || matchesPattern(entry.name, pattern)) {
                    results.push({
                        path: fullPath,
                        name: entry.name
                    });
                }
            }
        }
    }
    
    await traverse(directory);
    return results;
}

/**
 * Simple glob pattern matching
 * @param {string} filename - Filename to check
 * @param {string} pattern - Pattern to match
 * @returns {boolean}
 */
function matchesPattern(filename, pattern) {
    const regex = new RegExp('^' + pattern.replace(/\*/g, '.*').replace(/\?/g, '.') + '$');
    return regex.test(filename);
}

/**
 * Print directory tree
 * @param {string} directory - Root directory
 * @param {string} prefix - Indentation prefix
 */
async function printTree(directory, prefix = '') {
    let entries;
    try {
        entries = await fs.readdir(directory, { withFileTypes: true });
    } catch (error) {
        console.log(`Directory not found: ${directory}`);
        return;
    }
    
    // Sort: directories first, then files
    entries.sort((a, b) => {
        if (a.isDirectory() && !b.isDirectory()) return -1;
        if (!a.isDirectory() && b.isDirectory()) return 1;
        return a.name.localeCompare(b.name);
    });
    
    for (let i = 0; i < entries.length; i++) {
        const entry = entries[i];
        const isLast = i === entries.length - 1;
        const connector = isLast ? '└── ' : '├── ';
        
        console.log(`${prefix}${connector}${entry.name}`);
        
        if (entry.isDirectory()) {
            const extension = isLast ? '    ' : '│   ';
            await printTree(path.join(directory, entry.name), prefix + extension);
        }
    }
}

// Main
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length < 1) {
        console.log('Usage: node read_chinese_file.js <filepath>');
        console.log('   or: node read_chinese_file.js -d <directory> [pattern]');
        console.log("Example: node read_chinese_file.js '文档/报告.txt'");
        process.exit(1);
    }
    
    try {
        if (args[0] === '-d') {
            const directory = args[1];
            const pattern = args[2] || '*';
            
            console.log(`\nDirectory tree of: ${directory}\n`);
            await printTree(directory);
            
            console.log(`\nFiles matching '${pattern}':`);
            const files = await traverseChineseDir(directory, pattern);
            
            if (files.length === 0) {
                console.log('  (No files found)');
            } else {
                for (const file of files) {
                    console.log(`  ${file.name}`);
                    console.log(`    → ${file.path}`);
                }
            }
        } else {
            const content = await readChineseFile(args[0]);
            console.log(`Successfully read: ${args[0]}`);
            console.log('-'.repeat(40));
            console.log(content.substring(0, 500));
            if (content.length > 500) {
                console.log('...');
            }
        }
    } catch (error) {
        console.error(`Error: ${error.message}`);
        process.exit(1);
    }
}

main();
