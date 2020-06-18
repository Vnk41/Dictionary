import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DB{
    private Connection connection;

    public DB() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=WordCounter;user=user;password=cherry;");
    }

    //скачивание и выгрузка БД
    public void UploadDictionary (Main.Dictionary UploadingDictionary) throws SQLException {
        connection.setAutoCommit(false);
        try {
            PreparedStatement AddToFileData = connection.prepareStatement("INSERT INTO FileData(FileName, FilePath) VALUES( ? , ? )");
            PreparedStatement AddToWord = connection.prepareStatement("INSERT INTO Word(WordName) VALUES( ? )");
            PreparedStatement AddToWordInFile = connection.prepareStatement("INSERT INTO WordInFile(FileId, WordId, WordCounter) VALUES(?, ?, ?)");

            //проверка на наличие файла
            int TextFileID = GetTextFileID(UploadingDictionary.GetFileName());
            if (TextFileID == 0){
                AddToFileData.setString(1, UploadingDictionary.GetFileName());
                AddToFileData.setString(2, UploadingDictionary.GetFilePath());
                AddToFileData.execute();
                TextFileID = GetTextFileID(UploadingDictionary.GetFileName());
            }

            Map<String, Integer> dict = UploadingDictionary.GetDictionary();
            int finalTextFileID = TextFileID;
            dict.forEach( (Key, Value) -> {
                try{
                    int wordID = GetWordID(Key);
                    if (wordID == 0){
                        AddToWord.setString(1, Key);
                        AddToWord.execute();
                        wordID = GetWordID(Key);
                    }
                    AddToWordInFile.setInt(1, finalTextFileID);
                    AddToWordInFile.setInt(2, wordID);
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

    public Main.Dictionary DownloadDictionary(int TextFileID){
        String fileName = GetTextFileName(TextFileID);
        String filePath = GetTextFilePath(TextFileID);
        Map<String, Integer> dict = GetMapOfDictionary(TextFileID);
        if (fileName != null && filePath != null && dict != null) {
            return new Main.Dictionary(filePath, fileName, dict);
        }
        else return null;
    }

    public Main.Dictionary DownloadDictionary(String fileName){
        return DownloadDictionary(GetTextFileID(fileName));
    }

    //запросы

    public int GetTextFileID(String fileName){
        try {
            PreparedStatement Statement = connection.prepareStatement("SELECT Id FROM FileData WHERE FileName = ?");
            Statement.setString(1, fileName);
            ResultSet Res = Statement.executeQuery();
            if(Res.next()) {
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
            if(Res.next()) {
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
            if(Res.next()) {
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
            ResultSet rs = Statement.executeQuery();
            if(rs.next()) {
                return rs.getString("FilePath");
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
            ResultSet rs = Statement.executeQuery();
            if(rs.next()) {
                return rs.getInt("WordCounter");
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
            ResultSet rs = Statement.executeQuery();
            while (rs.next()) {
                WordsNumber += rs.getInt("WordCounter");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return WordsNumber;
    }
    public int GetWordsNumberInFile(String fileName){
        return GetWordsNumberInFile(GetTextFileID(fileName));
    }
    public int GetUniqueWordsNumberInFile(String fileName){
        return GetUniqueWordsNumberInFile(GetTextFileID(fileName));
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
    public Map<String, Integer> GetMapOfDictionary(String fileName){
        return GetMapOfDictionary(GetTextFileID(fileName));
    }
    public Map<String, Integer> GetMapOfDictionary(int TextFileID){
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
            map = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
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
