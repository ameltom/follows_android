package tom.androidapp;

/**
 * Created by tomamel on 27/04/16.
 */
public class PersonData {
    public String name;
    public int batteryLevel;
    public int batteryScale;
    public double GPSLatitude;
    public double GPSLongitude;
    public long updatedAtSeconds;

    public PersonData(String name, int batteryLevel, int batteryScale, double GPSLatitude, double GPSLongitude, long updatedAtSeconds) {
        this.name = name;
        this.batteryLevel = batteryLevel;
        this.batteryScale = batteryScale;
        this.GPSLatitude = GPSLatitude;
        this.GPSLongitude = GPSLongitude;
        this.updatedAtSeconds = updatedAtSeconds;
    }

    public float batteryPercentage(){
        return batteryLevel / (float) batteryScale;
    }

    public String toViewString(){
        return name + ": " + Math.round(batteryPercentage() * 100) + "%";
    }

}
