public class GeneralFile {

    private int id;
    private String name;
    private long datalength;
    private String fileExtension;
    private String path;

    public GeneralFile(int id, String name, long datalength, String fileExtension, String path) {
        this.id = id;
        this.name = name;
        this.datalength = datalength;
        this.fileExtension = fileExtension;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public long getDataLength() {
        return datalength;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

}