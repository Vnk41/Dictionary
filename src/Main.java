import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main{

    public static class Dictionary{
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

                    Future<Map<String, Integer>> TextPart = executor.submit(new DictionaryMaker(SubStrings[i]));
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
                    Future<Map<String, Integer>> TextPart = executor.submit(new DictionaryMaker(SubStrings[i]));
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
        }
    }

    public static class DB{
        private Connection connection;

        public DB() throws SQLException {
            connection = DriverManager.getConnection("jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=WordCounter;user=user;password=cherry;");
        }

        //скачивание и выгрузка БД
        public void UploadDictionary (Dictionary UploadingDictionary) throws SQLException {
            connection.setAutoCommit(false);

            String FileDataSqlQuery = "INSERT INTO FileData(FileName, FilePath) VALUES( ? , ? )";
            String WordSqlQuery = "INSERT INTO Word(WordName) VALUES( ? )";
            String WordInFileSqlQuery = "INSERT INTO WordInFile(FileId, WordId, WordCounter) VALUES(?, ?, ?)";

            Map<String, Integer> dict = UploadingDictionary.GetDictionary();
            int TextFileID = GetTextFileID(UploadingDictionary.GetFileName());
            int FinalTextFileID = TextFileID;
            final int[] WordID = {0};

            try {
                PreparedStatement AddToFileData = connection.prepareStatement(FileDataSqlQuery);
                PreparedStatement AddToWord = connection.prepareStatement(WordSqlQuery);
                PreparedStatement AddToWordInFile = connection.prepareStatement(WordInFileSqlQuery);
                if (TextFileID == 0){
                    AddToFileData.setString(1, UploadingDictionary.GetFileName());
                    AddToFileData.setString(2, UploadingDictionary.GetFilePath());
                    AddToFileData.execute();
                    TextFileID = GetTextFileID(UploadingDictionary.GetFileName());
                    TextFileID = GetTextFileID(UploadingDictionary.GetFileName());
                }
                dict.forEach( (Key, Value) -> {
                    try{
                        WordID[0] = GetWordID(Key);
                        if (WordID[0] == 0){
                            AddToWord.setString(1, Key);
                            AddToWord.execute();
                            WordID[0] = GetWordID(Key);
                        }
                        AddToWordInFile.setInt(1, FinalTextFileID);
                        AddToWordInFile.setInt(2, WordID[0]);
                        AddToWordInFile.setInt(3, Value);
                        AddToWordInFile.execute();
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                });
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
        }

        public Dictionary DownloadDictionary(int TextFileID){
            String fileName = GetTextFileName(TextFileID);
            String filePath = GetTextFilePath(TextFileID);
            Map<String, Integer> dict = GetDictionary(TextFileID);
            if (fileName != null && filePath != null && dict != null) {
                return new Dictionary(filePath, fileName, dict);
            }
            else return null;
        }

        public Dictionary DownloadDictionary(String fileName){
            return DownloadDictionary(GetTextFileID(fileName));
        }

        //запросы

        public int GetTextFileID(String fileName){
            try {
                PreparedStatement stm = connection.prepareStatement("select Id from FileData where FileName = ?");
                stm.setString(1, fileName);
                ResultSet Res = stm.executeQuery();
                if(Res.next() == true) {
                    return Res.getInt("Id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }
        public int GetWordID(String word){
            try {
                //…
                PreparedStatement stm = connection.prepareStatement("select Id from Word where WordName = ?");
                stm.setString(1, word);
                ResultSet rs = stm.executeQuery();
                while(rs.next()) {
                    return rs.getInt("Id");
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }
        public String GetTextFileName(int TextFileID){
            try {
                PreparedStatement stm = connection.prepareStatement("select FileName from FileData where Id = ?");
                stm.setInt(1, TextFileID);
                ResultSet Res = stm.executeQuery();
                if(Res.next() == true) {
                    return Res.getString("FileName");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public String GetTextFilePath(int TextFileID){
            try {
                PreparedStatement stm = connection.prepareStatement("select FilePath from FileData where Id = ?");
                stm.setInt(1, TextFileID);
                ResultSet Res = stm.executeQuery();
                if(Res.next() == true) {
                    return Res.getString("FilePath");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public int GetWordCountInFile(String fileName, String word){
            return GetWordCountInFile(GetTextFileID(fileName), word);
        }
        public int GetWordCountInFile(int TextFileID, String word){
            int WordID = GetWordID(word);
            if (TextFileID == 0 || WordID == 0) return 0;
            try {
                PreparedStatement stm = connection.prepareStatement("select WordCounter from WordInFile where FileId = ? and WordId = ?");
                stm.setInt(1, TextFileID);
                stm.setInt(2, WordID);
                ResultSet Res = stm.executeQuery();
                if(Res.next() == true) {
                    return Res.getInt("WordCounter");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }

        public int GetWordsNumberInFile(int TextFileID){
            if (TextFileID == 0) return 0;
            int WordsNumber = 0;
            try {
                PreparedStatement stm = connection.prepareStatement("select WordCounter from WordInFile where FileId = ?");
                stm.setInt(1, TextFileID);
                ResultSet Res = stm.executeQuery();
                while (Res.next() == true) {
                    WordsNumber += Res.getInt("WordCounter");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return WordsNumber;
        }
        public int GetWordsNumberInFile(String fileName){
            return GetWordsNumberInFile(GetTextFileID(fileName));
        }
        public int GetUniqueWordsNumberInFile(int TextFileID){
            int UniqueWordsNumber = 0;
            try {
                PreparedStatement stm = connection.prepareStatement("select WordCounter from WordInFile where FileId = ?");
                stm.setInt(1, TextFileID);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    UniqueWordsNumber++;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return UniqueWordsNumber;
        }
        public int GetUniqueWordsNumberInFile(String fileName){
            return GetUniqueWordsNumberInFile(GetTextFileID(fileName));
        }
        public Map<String, Integer> GetDictionary(String fileName){
            return GetDictionary(GetTextFileID(fileName));
        }
        public Map<String, Integer> GetDictionary(int TextFileID){
            Map<String, Integer> map = new HashMap<String, Integer>();
            try {
                PreparedStatement stm = connection.prepareStatement("select w.WordName, wif.WordCounter from  WordInFile wif inner join Word w ON wif.FileId = w.Id where wif.FileId = ?");
                stm.setInt(1, TextFileID);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    map.put(rs.getString("WordName"), rs.getInt("WordCounter"));
                }
                return map;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public Map<String, Integer> GetFilenamesWhereWordExists(String word){
            return GetFilenamesWhereWordExists(GetWordID(word));
        }
        public Map<String, Integer> GetFilenamesWhereWordExists(int wordID){
            Map<String, Integer> map = new HashMap<String, Integer>();
            try {
                PreparedStatement stm = connection.prepareStatement("select fd.FileName, wif.WordCounter from  WordInFile wif inner join FileData fd ON wif.FileId = fd.Id where wif.WordId = ?");
                stm.setInt(1, wordID);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    map.put(rs.getString("FileName"), rs.getInt("WordCounter"));
                }
                return map;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        public List<String> GetAvailableWords(){
            List<String> list = new ArrayList<String>();
            try {
                PreparedStatement stm = connection.prepareStatement("select WordName from Word order by WordName");
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    list.add(rs.getString("WordName"));
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }



    public static void main(String[] args) throws IOException, SQLException {

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        DB Database = new DB();
        Dictionary dict = new Dictionary(new File("input1.txt"), 4);
        Database.UploadDictionary(dict);
    }
}
