import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;

/**
 * Read file with Chinese (Unicode) path support.
 * Usage: java ReadChineseFile <filepath>
 */
public class ReadChineseFile {
    
    /**
     * Read a file with Chinese characters in path.
     * 
     * @param filepath Path to file (may contain Chinese characters)
     * @return File content as string
     * @throws IOException If file cannot be read
     */
    public static String readChineseFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filepath);
        }
        
        if (!Files.isRegularFile(path)) {
            throw new IOException("Path is not a file: " + filepath);
        }
        
        // Explicitly use UTF-8 encoding
        return Files.readString(path, Charset.forName("UTF-8"));
    }
    
    /**
     * Traverse directory containing Chinese filenames.
     * 
     * @param directory Root directory path
     * @throws IOException If directory cannot be traversed
     */
    public static void traverseChineseDir(String directory) throws IOException {
        Path root = Paths.get(directory);
        
        if (!Files.exists(root)) {
            throw new IOException("Directory not found: " + directory);
        }
        
        if (!Files.isDirectory(root)) {
            throw new IOException("Path is not a directory: " + directory);
        }
        
        System.out.println("Traversing: " + directory);
        
        Files.walk(root).forEach(path -> {
            String indent = "  ".repeat(path.getNameCount() - root.getNameCount());
            if (Files.isDirectory(path)) {
                System.out.println(indent + "[" + path.getFileName() + "]");
            } else {
                System.out.println(indent + path.getFileName());
            }
        });
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ReadChineseFile <filepath>");
            System.out.println("   or: java ReadChineseFile -d <directory>");
            System.out.println("Example: java ReadChineseFile '文档/报告.txt'");
            System.exit(1);
        }
        
        try {
            if (args[0].equals("-d") && args.length > 1) {
                traverseChineseDir(args[1]);
            } else {
                String content = readChineseFile(args[0]);
                System.out.println("Successfully read: " + args[0]);
                System.out.println("-".repeat(40));
                System.out.println(content.substring(0, Math.min(500, content.length())));
                if (content.length() > 500) {
                    System.out.println("...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
