import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@WebServlet(name = "/servlet")
@MultipartConfig
public class Servlet extends javax.servlet.http.HttpServlet {
    public class SQLloaderJAR{
        SQLloaderJAR(){
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    SQLloaderJAR sqlLoaderJAR = new SQLloaderJAR();
    private static class MyConsoleOutput{
        static String OutputData = "";
        public static void Println(String string){
            OutputData += "<p>" + string + "</p>";
        }
        public static void PrintlnDictionaryFromWordExist(String string){
            OutputData += "<p id = 'button 'class='dictionaryButton' onclick='getDictionary(\"" + string + "\")'> Переход к частотному словарю </ <p></p> <p></p>";
        }
        public static void ClearBuff(){
            OutputData = "";
        }
        public static String GetOutputData (){
            return OutputData;
        }
    };
    private boolean SaveFile(String Filename, byte[] bytes){
        Main.Dictionary Dict = new Main.Dictionary(Filename, Filename, new String(bytes, StandardCharsets.UTF_8), 4);
        try {
            DB Database = new DB();
            Database.UploadDictionary(Dict);
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String GetDictionary(String Filename){
        Main.Dictionary Dict = null;
        try {
            DB Database = new DB();
            Dict = Database.DownloadDictionary(Filename);
        }catch (SQLException e){
            e.printStackTrace();
        }
        MyConsoleOutput console = new MyConsoleOutput();
        Map<String, Integer> dict = Dict.GetDictionary();
        console.ClearBuff();
        console.Println("File name: " + Dict.GetFileName());
        console.Println("Number Of Words: " + Dict.GetNumberOfAllWords());
        console.Println("Number Of Unique words: " + Dict.GetNumberOfUniqueWords());
        console.Println("Dictionary: ");
        dict.forEach( (Key, Value)->console.Println(Key + " - " + Value ) );
        return console.GetOutputData();
    }
    private String GetWordsList(){
        List<String> WordsList;
        try {
            DB Database = new DB();
            WordsList = Database.GetAvailableWords();
        }catch (SQLException e){
            e.printStackTrace();
            return "";
        }
        MyConsoleOutput console = new MyConsoleOutput();
        console.ClearBuff();
        WordsList.forEach((word)-> console.Println(word) );
        return console.GetOutputData();

    }
    private String GetFilesListWhereWordExist(String word){
        Map<String, Integer> FilesList;
        try {
            DB Database = new DB();
            FilesList = Database.GetFilenamesWhereWordExists(word);
        }catch (SQLException e){
            e.printStackTrace();
            return "";
        }
        if (FilesList.size() == 0) return "";

        MyConsoleOutput console = new MyConsoleOutput();
        console.ClearBuff();

        try {
            DB Database = new DB();
            FilesList.forEach((filename, wordCount) ->{
                    console.Println("File Name: " + filename);
                    console.Println("This Word Count: " + wordCount);
                    console.Println("Number Of Words: " + Database.GetWordsNumberInFile(filename));
                    console.PrintlnDictionaryFromWordExist(filename);
            });
        }catch (SQLException e){
            e.printStackTrace();
            return "";
        }
        return console.GetOutputData();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String Choice = "", FileName="", Word="", Response = "";
        byte[] FileBytes = new byte[0];
        
        Collection <Part> parts = request.getParts();
        
        for (Part part: parts){
            byte[] bytes = new byte[(int) part.getSize()];
            System.out.println(part);
            try {
                InputStream is = part.getInputStream();
                is.read(bytes,0, (int) part.getSize());
                System.out.println(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch (part.getName()){
                case "command" : {
                    Choice = new String(bytes, UTF_8);
                    break;}
                case "filename" : {
                    FileName = new String(bytes, UTF_8);
                    break;}
                case "word" : {
                    Word = new String(bytes, UTF_8);
                    break;}
                case "file" : {
                    FileBytes = bytes;
                    break;}
                default: break;
            }
        }
        switch (Choice){
            case "uploadFile":{
                if (SaveFile(FileName, FileBytes) == true)
                    Response = "Файл успешно добавлен";
                else
                    Response = "Файл не удалось добавить";
                break;}
            case "getDictionary":{
                Response = GetDictionary(FileName);
                break;}
            case "getAllWords":{
                Response = GetWordsList();
                break;}
            case "getFilesWhereWordExist":{
                Response = GetFilesListWhereWordExist(Word);
                break;}
            default: Response = ""; break;
        }
        PrintWriter pw = response.getWriter();
        pw.write(Response);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws javax.servlet.ServletException, IOException {
    }
}
