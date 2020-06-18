import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Dictionary{
    private Map<String, Integer> UnitedDictionary = new HashMap<String, Integer>();
    private String Text;
    private String Path;
    private String Name;
    private int TotalWordCounter = 0;
    private int ThreadTextStart = 0;
    private int ThreadTextEnd = 0;

    void CreateDictionary(int NumberOfThreads){
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Map<String, Integer>>> LocalDictionaries = new ArrayList<>();

        String[] textParts = new String[NumberOfThreads];
        int NumberOfSymbolsInText = Text.length();
        try{
            for(int i = 0; i < NumberOfThreads; i++){
                ThreadTextStart = ThreadTextEnd;
                ThreadTextEnd = (i+1)/NumberOfThreads*NumberOfSymbolsInText;
                if (i == NumberOfThreads - 1) {
                    ThreadTextEnd = NumberOfSymbolsInText - 1;
                }
                if (Text.charAt(ThreadTextEnd) == ' ') {
                    while (Text.charAt(ThreadTextEnd) != ' ')
                        ThreadTextEnd++;
                }
                textParts[i] = Text.substring(ThreadTextStart, ThreadTextEnd);

                Future<Map<String, Integer>> TextPart = executor.submit(new Main.Dictionary.DictionaryMaker(textParts[i]));
                LocalDictionaries.add(TextPart);
                Map<String, Integer> localDictionary = LocalDictionaries.get(i).get();
                localDictionary.forEach( (Key, Value) -> UnitedDictionary.merge(Key, Value, (PreValue, CurValue) -> PreValue + CurValue));
            }
            UnitedDictionary.forEach( (Key, Value) -> TotalWordCounter += Value);

        }
        catch (InterruptedException | ExecutionException exception){

        }
        finally {
            executor.shutdown();
        }
    }

    public static class DictionaryMaker implements Callable<Map<String, Integer>> {
        private String LocalText;
        private int LocalCounter;

        private Map<String, Integer> MakeDictionary(String text){
            Map<String, Integer> LocalDictionary = new HashMap<String, Integer>();
            String text_LowerCase = text.toLowerCase();
            String[] words = text_LowerCase.replaceAll("[0123456789.,;:|+-<>(){}/|_–”«»='\"`~!@#№$%^&?—*]"," ")
                    .split("\\s+");
            for(String word: words){
                if (word.isEmpty() == false){
                    if (LocalDictionary.containsKey(word)){
                        LocalCounter = LocalDictionary.get(word);
                        LocalCounter++;
                        LocalDictionary.replace(word, LocalCounter);
                    }
                    else{
                        LocalDictionary.put(word, 1);
                    }
                }
            }
            return LocalDictionary;
        }
        public DictionaryMaker(String textPart){
            LocalText = textPart;
        }
        @Override
        public Map<String, Integer> call() throws Exception {
            return MakeDictionary(LocalText);
        }
    }

    public Dictionary(File file, int threads) throws IOException {
        Text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Path = file.getAbsolutePath();
        Name = file.getName();
        CreateDictionary(threads);
    }
    public Dictionary(String _FilePath, String _FileName, String InitText, int threads){
        Path = _FilePath;
        Name = _FileName;
        Text = InitText;
        CreateDictionary(threads);
    }
    public Dictionary(String _filePath, String _fileName, Map<String, Integer> _dictionary){
        Path = _filePath;
        Name = _fileName;
        UnitedDictionary.putAll(_dictionary);
        UnitedDictionary.forEach( (Key, Value) -> TotalWordCounter += Value);
    }

    public String GetFilePath(){
        return Path;
    }
    public String GetFileName(){
        return Name;
    }
    public Map<String, Integer>  GetDictionary() {
        return UnitedDictionary;
    }
    public int GetNumberOfUniqueWords(){
        return UnitedDictionary.size();
    }
    public int GetNumberOfAllWords(){
        return TotalWordCounter;
    }
}