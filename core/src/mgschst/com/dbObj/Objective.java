package mgschst.com.dbObj;

public class Objective {
    int id;
    int duration;
    String type;
    public Objective(int newID){
        this.id = newID;
        switch (id){
            case 0 -> duration = 0;
            case 6 -> {
                duration = 1;
                type = "easy";
            }
            case 54 -> {
                duration = 2;
                type = "normal";
            }
            case 55 -> {
                duration = 2;
                type = "easy";
            }
            case 56 -> {
                duration = 3;
                type = "hard";
            }
        }

    }
    public int getId() {
        return id;
    }
    public int getDuration() {
        return duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public boolean equals(int id) {
        return this.id == id;
    }
}
