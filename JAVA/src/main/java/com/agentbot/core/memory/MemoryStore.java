package com.agentbot.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MemoryStore manages persistent memory files.
 * 
 * Files:
 * - memory/MEMORY.md: Long-term curated memory
 * - memory/memory.log: Daily event log
 */
public class MemoryStore {
  private static final Logger log = LoggerFactory.getLogger(MemoryStore.class);
  private final Path memoryDir;

  public MemoryStore(Path memoryDir) {
    this.memoryDir = memoryDir;
  }

  /**
   * Read long-term memory from MEMORY.md.
   */
  public List<String> readLongTerm() {
    Path memoryFile = memoryDir.resolve("MEMORY.md");
    if (!Files.exists(memoryFile)) {
      log.debug("MEMORY.md not found at: {}", memoryFile);
      return Collections.emptyList();
    }
    try {
      return Files.readAllLines(memoryFile);
    } catch (IOException e) {
      log.error("Failed to read MEMORY.md", e);
      return Collections.emptyList();
    }
  }

  /**
   * Read daily memory log.
   */
  public List<String> readDaily() {
    Path memoryFile = memoryDir.resolve("memory.log");
    if (!Files.exists(memoryFile)) return Collections.emptyList();
    try {
      return Files.readAllLines(memoryFile);
    } catch (IOException e) {
      log.error("Failed to read memory.log", e);
      return Collections.emptyList();
    }
  }

  /**
   * Read all memory (long-term + daily).
   */
  public List<String> readAll() {
    List<String> combined = new ArrayList<>();
    combined.addAll(readLongTerm());
    combined.addAll(readDaily());
    return combined;
  }

  /**
   * Append a line to the daily memory log.
   */
  public void appendDaily(String line) {
    try {
      Files.createDirectories(memoryDir);
      Path file = memoryDir.resolve("memory.log");
      Files.writeString(file, line + System.lineSeparator(), 
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      log.error("Failed to append to memory.log", e);
    }
  }

  /**
   * Append content to a specific section in MEMORY.md.
   * 
   * @param section Section name (e.g., "User Information")
   * @param content Content to append
   * @return true if successful
   */
  public boolean appendToMemoryMd(String section, String content) {
    Path memoryFile = memoryDir.resolve("MEMORY.md");
    
    try {
      Files.createDirectories(memoryDir);
      
      if (!Files.exists(memoryFile)) {
        // Create new MEMORY.md with basic structure
        initializeMemoryMd(memoryFile);
      }
      
      String fileContent = Files.readString(memoryFile);
      
      // Find the section
      String sectionMarker = "## " + section;
      int sectionIndex = fileContent.indexOf(sectionMarker);
      
      if (sectionIndex == -1) {
        // Section doesn't exist, append it at the end
        fileContent += String.format("\n\n## %s\n\n%s\n", section, content);
      } else {
        // Find the next section or end of file
        int nextSectionIndex = fileContent.indexOf("\n## ", sectionIndex + sectionMarker.length());
        
        if (nextSectionIndex == -1) {
          // This is the last section
          fileContent += "\n" + content + "\n";
        } else {
          // Insert before the next section
          String before = fileContent.substring(0, nextSectionIndex);
          String after = fileContent.substring(nextSectionIndex);
          fileContent = before + "\n" + content + "\n" + after;
        }
      }
      
      Files.writeString(memoryFile, fileContent);
      log.info("Appended to MEMORY.md section '{}': {}", section, content);
      return true;
      
    } catch (IOException e) {
      log.error("Failed to append to MEMORY.md", e);
      return false;
    }
  }

  /**
   * Initialize MEMORY.md with basic structure.
   */
  private void initializeMemoryMd(Path memoryFile) throws IOException {
    String template = """
        # Long-term Memory
        
        ## User Information
        
        (Important facts about the user)
        
        ## Preferences
        
        (User preferences learned over time)
        
        ## Project Context
        
        (Information about ongoing projects)
        
        ## Important Notes
        
        (Things to remember)
        
        ---
        
        *This file is automatically updated by AgentBot when important information should be remembered.*
        """;
    
    Files.writeString(memoryFile, template);
    log.info("Initialized MEMORY.md at: {}", memoryFile);
  }

  /**
   * Search memory for a keyword.
   */
  public List<String> search(String keyword) {
    List<String> allLines = readAll();
    String lowerKeyword = keyword.toLowerCase();
    
    return allLines.stream()
        .filter(line -> line.toLowerCase().contains(lowerKeyword))
        .collect(Collectors.toList());
  }

  /**
   * Clear the daily memory log.
   */
  public void clearDaily() {
    Path file = memoryDir.resolve("memory.log");
    try {
      if (Files.exists(file)) {
        Files.delete(file);
        log.info("Cleared daily memory log");
      }
    } catch (IOException e) {
      log.error("Failed to clear memory.log", e);
    }
  }

  /**
   * Get memory statistics.
   */
  public MemoryStats getStats() {
    List<String> longTerm = readLongTerm();
    List<String> daily = readDaily();
    
    long longTermSize = 0;
    long dailySize = 0;
    
    try {
      Path longTermFile = memoryDir.resolve("MEMORY.md");
      if (Files.exists(longTermFile)) {
        longTermSize = Files.size(longTermFile);
      }
      
      Path dailyFile = memoryDir.resolve("memory.log");
      if (Files.exists(dailyFile)) {
        dailySize = Files.size(dailyFile);
      }
    } catch (IOException e) {
      log.error("Failed to get file sizes", e);
    }
    
    return new MemoryStats(longTerm.size(), daily.size(), longTermSize, dailySize);
  }
}
