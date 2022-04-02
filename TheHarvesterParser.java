import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TheHarvesterParser {
    public static void main(String[] args) {
        String domain = args[0];
        String searchEngines = args[1];
        int limit = Integer.parseInt(args[2]);
        String outputPath = args[3];

        String rawResult = runHarvester(domain, searchEngines, limit);

        Map<String, String> subdomainToIpMapping = parseHarvesterResult(rawResult);

        try {
            writeToJsonFile(subdomainToIpMapping, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToJsonFile(Map<String, String> subdomainToIpMapping, String filePath) throws IOException {
        StringBuilder retVal = new StringBuilder("{\n");

        for (Map.Entry<String, String> entry : subdomainToIpMapping.entrySet()) {
            retVal.append(String.format("\"%s\"", entry.getKey()))
                    .append(":")
                    .append(String.format("\"%s\"", entry.getValue()))
                    .append("\n");
        }

        retVal.append("}");

        File outputFile = new File(filePath);
        FileWriter writer = new FileWriter(filePath);
        writer.write(retVal.toString());
        writer.flush();
        writer.close();
    }

    // domainName => ip1,ip2,ip...
    private static Map<String, String> parseHarvesterResult(String rawResult) {
        List<String> lst = getHostsList(rawResult);

        Map<String, String> domainToIpsMapping = lst.stream().filter(s -> !s.trim().isEmpty()).
                map(line -> line.split(":")).
                collect(Collectors
                        .groupingBy(line -> line[0],
                                Collectors.mapping(line -> line.length == 2 ? line[1] : "",
                                        Collectors.joining())));

        return domainToIpsMapping;
    }

    private static List<String> getHostsList(String rawResult) {
        String[] splitted = rawResult.split("Hosts found: [0-9]*\n" +
                "---------------------");

        String rawHostsList = splitted[1];

        if (rawHostsList != null && !rawHostsList.isEmpty()) {
            return Arrays.asList(rawHostsList.split("\n"));
        } else {
            return null;
        }
    }

    private static String runHarvester(String domain, String searchEngines, Integer limit) {
        String commandToRun = String.format("python3 theHarvester.py -d %s -l %d -b %s", domain, limit, searchEngines);
        System.out.println("Running command: " + commandToRun);
        String rawOutput = execCmd(commandToRun);

        return rawOutput;
    }

    public static String execCmd(String cmd) {
        String result = null;

        try (InputStream inputStream = Runtime.getRuntime().exec(cmd, null, new File("/home/almogi/TheHarvesterParser/theHarvester/")).getInputStream();
             Scanner s = new Scanner(inputStream).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
