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

    public Main.Dictionary DownloadDictionary(int TextFileID){
        String fileName = GetTextFileName(TextFileID);
        String filePath = GetTextFilePath(TextFileID);
        Map<String, Integer> dict = GetDictionary(TextFileID);
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