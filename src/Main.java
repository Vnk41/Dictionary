import java.io.File;
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

                    Future<Map<String, Integer>> TextPart = executor.submit(new DictionaryMaker(textParts[i]));
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
                PreparedStatement Statement = connection.prepareStatement("SELECT Id FROM FileData WHERE FileName = ?");
                Statement.setString(1, fileName);
                ResultSet Res = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT Id FROM Word WHERE WordName = ?");
                Statement.setString(1, word);
                ResultSet Res = Statement.executeQuery();
                if(Res.next() == true) {
                    return Res.getInt("Id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }
        public String GetTextFileName(int TextFileID){
            try {
                PreparedStatement Statement = connection.prepareStatement("SELECT FileName FROM FileData WHERE Id = ?");
                Statement.setInt(1, TextFileID);
                ResultSet Res = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT FilePath FROM FileData WHERE Id = ?");
                Statement.setInt(1, TextFileID);
                ResultSet Res = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT WordCounter FROM WordInFile WHERE FileId = ? AND WordId = ?");
                Statement.setInt(1, TextFileID);
                Statement.setInt(2, WordID);
                ResultSet Res = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT WordCounter FROM WordInFile WHERE FileId = ?");
                Statement.setInt(1, TextFileID);
                ResultSet Res = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT WordCounter FROM WordInFile WHERE FileId = ?");
                Statement.setInt(1, TextFileID);
                ResultSet rs = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT \n" +
                        "\tw.WordName,\n" +
                        "\tWIF.WordCounter\n" +
                        "FROM WordInFile WIF JOIN Word w ON WIF.WordId = w.Id\n" +
                        "WHERE WIF.FileId = ?\n" +
                        "ORDER BY WordName");
                Statement.setInt(1, TextFileID);
                ResultSet rs = Statement.executeQuery();
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
                PreparedStatement Statement = connection.prepareStatement("SELECT\n" +
                        "\tFD.fileName,\n" +
                        "\tWIF.WordCounter\n" +
                        "FROM WordInFile WIF JOIN FileData FD ON WIF.FileId = FD.Id\n" +
                        "WHERE WIF.WordId = ?\n" +
                        "ORDER BY FD.FileName");
                Statement.setInt(1, wordID);
                ResultSet rs = Statement.executeQuery();
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
                PreparedStatement stm = connection.prepareStatement("SELECT WordName FROM Word ORDER BY WordName");
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
