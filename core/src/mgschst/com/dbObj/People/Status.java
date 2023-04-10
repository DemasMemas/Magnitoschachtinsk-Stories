package mgschst.com.dbObj.People;

public class Status {
    int id;
    String name;
    int duration;
    String pictureName;

    public Status(int id, String name, int duration, String pictureName) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.pictureName = pictureName;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPictureName() {
        return pictureName;
    }
}
