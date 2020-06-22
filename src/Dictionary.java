import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

public class Dictionary{
    private Map<String, Integer> UnitedDictionary = new HashMap<String, Integer>();
    private String FileName;
    private String FilePath;
    private String TextFile;
    private int TotalWordCounter = 0;
    private static int WordCounter = 0;

    void CreateDictionary(int NumberOfThreads, File file){
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Map<String, Integer>>> LocalDictionaries = new ArrayList<>();
        int StringStart = 0;
        int StringEnd = 0, StringEndPos = 0;

        String[] SubStrings = new String[NumberOfThreads];
        String AllFileTextInLine = "";//сбор текста в строку
        Scanner Scanner = null;
        try {
            Scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(Scanner.hasNext()) {
            AllFileTextInLine += Scanner.nextLine() + " ";
        }
        System.out.println(AllFileTextInLine);
        int WordDivieCounter = 0;


        for (int i = 0; i < AllFileTextInLine.length(); i++) {
            if (AllFileTextInLine.charAt(i) == ' ') {
                WordDivieCounter++;
            }
        }
        int NumberOfWords = WordDivieCounter;
        WordDivieCounter = 0;
        for (int i = 0; i < NumberOfThreads; i++) {
            StringStart = StringEndPos;
            StringEnd = (i+1)*NumberOfWords/NumberOfThreads;
            if (i == NumberOfThreads - 1) {
                StringEnd = NumberOfWords;
            }
            for (int j = 0; j < AllFileTextInLine.length(); j++) {
                if (AllFileTextInLine.charAt(j) == ' ') {
                    WordDivieCounter++;
                    //System.out.println(WordCounterCounter);
                }
                if (WordDivieCounter == StringEnd) {
                    StringEndPos = j;
                    break;
                }
            }
            WordDivieCounter = 0;
            SubStrings[i] = AllFileTextInLine.substring(StringStart, StringEndPos);
            System.out.println(SubStrings[i]);

        }
        try{
            for(int i = 0; i < NumberOfThreads; i++){

                Future<Map<String, Integer>> TextPart = executor.submit(new Main.Dictionary.DictionaryMaker(SubStrings[i]));
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
        for (Map.Entry UnitedDictionary : UnitedDictionary.entrySet()) {
            System.out.println("Key: " + UnitedDictionary.getKey() + " Value: "
                    + UnitedDictionary.getValue());
        }
        UnitedDictionary.forEach( (Key, Value) -> WordCounter += Value);
        System.out.println("Уникальных слов: " + UnitedDictionary.size() + "\nВсего слов: " + WordCounter);

    }

    void CreateDictionaryForUploadButton(int NumberOfThreads, String AllFileInTextUpload) {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Map<String, Integer>>> LocalDictionaries = new ArrayList<>();
        int StringStart = 0;
        int StringEnd = 0, StringEndPos = 0;
        int WordDivideCounter = 0;
        String[] SubStrings = new String[NumberOfThreads];
        for (int i = 0; i < AllFileInTextUpload.length(); i++) {
            if (AllFileInTextUpload.charAt(i) == ' ') {
                WordDivideCounter++;
            }
        }
        int NumberOfWords = WordDivideCounter;
        WordDivideCounter = 0;
        for (int i = 0; i < NumberOfThreads; i++) {
            StringStart = StringEndPos;
            StringEnd = (i+1)*NumberOfWords/NumberOfThreads;
            if (i == NumberOfThreads - 1) {
                StringEnd = NumberOfWords;
            }
            for (int j = 0; j < AllFileInTextUpload.length(); j++) {
                if (AllFileInTextUpload.charAt(j) == ' ') {
                    WordDivideCounter++;
                    //System.out.println(WordCounterCounter);
                }
                if (WordDivideCounter == StringEnd) {
                    StringEndPos = j;
                    break;
                }
            }
            WordDivideCounter = 0;
            SubStrings[i] = AllFileInTextUpload.substring(StringStart, StringEndPos);
            System.out.println(SubStrings[i]);
        }

        try{
            for(int i = 0; i < NumberOfThreads; i++){
                Future<Map<String, Integer>> TextPart = executor.submit(new Main.Dictionary.DictionaryMaker(SubStrings[i]));
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
        UnitedDictionary.forEach( (Key, Value) -> WordCounter += Value);
    }


    public static class DictionaryMaker implements Callable<Map<String, Integer>> {
        private String LocalThreadText;
        Map <String, Integer> FileHashMapDictionary = new HashMap<>();
        Map <String, Integer> ThreadDictionary = new HashMap<>();
        private Map<String, Integer> MakeDictionary (String TextThread){
            String [] words = TextThread.toLowerCase().replaceAll("[0123456789.,;:|+-<>(){}/|_–”«»='\\\"`~!@#№$%^&?—*]", "").split("\\s");
            Integer count = 0;
            for (String word : words) {
                if (word.isEmpty() == false) {
                    count = ThreadDictionary.get(word);
                    if(count == null) {
                        count = 0;
                    }
                    count++;
                    ThreadDictionary.put(word, count);
                }
            }
            System.out.println("Уникальных слов: " + ThreadDictionary.size() + "\nВсего слов: " + words.length);
            return ThreadDictionary;
        }

        public DictionaryMaker(String textPart){
            LocalThreadText = textPart;
        }
        @Override
        public Map<String, Integer> call() throws Exception {
            return MakeDictionary(LocalThreadText);
        }
    }

    public Dictionary(File file, int threads) throws IOException {
        TextFile = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        FilePath = file.getAbsolutePath();
        FileName = file.getName();
        CreateDictionary(threads, file);
    }
    public Dictionary(String _FilePath, String _FileName, String InitText, int threads){
        FilePath = _FilePath;
        FileName = _FileName;
        TextFile = InitText;
        CreateDictionaryForUploadButton(threads, TextFile);
    }
    public Dictionary(String _filePath, String _fileName, Map<String, Integer> _dictionary){
        FilePath = _filePath;
        FileName = _fileName;
        UnitedDictionary.putAll(_dictionary);
        UnitedDictionary.forEach( (Key, Value) -> TotalWordCounter += Value);
    }

    public String GetFilePath(){
        return FilePath;
    }
    public String GetFileName(){
        return FileName;
    }
    public Map<String, Integer>  GetDictionary(){
        return UnitedDictionary;
    }
    public int GetNumberOfUniqueWords(){
        return UnitedDictionary.size();
    }
    public int GetNumberOfAllWords(){
        return TotalWordCounter;
    }}