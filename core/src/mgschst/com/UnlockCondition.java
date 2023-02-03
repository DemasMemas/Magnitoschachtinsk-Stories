package mgschst.com;

public class UnlockCondition {
    private final String type;
    private final Integer cost;
    private final String productType;

    public UnlockCondition(String newType, Integer newCost, String newProductType){
        this.type = newType;
        this.cost = newCost;
        this.productType = newProductType;
    }

    public UnlockCondition(String newType, String newProductType){
        this.type = newType;
        this.cost = 0;
        this.productType = newProductType;
    }

    public String getType() {
        return type;
    }

    public Integer getCost() {
        return cost;
    }

    public String getProductType() {
        return productType;
    }

    public String userGet(){
        switch (type) {
            case "rank":
                String rank;
                if (cost <= 0) {
                    rank = "Залётный I";
                } else if (cost <= 1) {
                    rank = "Залётный II";
                } else if (cost <= 2) {
                    rank = "Дикий I";
                } else if (cost <= 3) {
                    rank = "Дикий II";
                } else if (cost <= 4) {
                    rank = "Дикий III";
                } else if (cost <= 5) {
                    rank = "Заводской";
                } else if (cost <= 6) {
                    rank = "Живой куст";
                } else if (cost <= 7) {
                    rank = "Решала";
                } else if (cost <= 8) {
                    rank = "Торговец";
                } else {
                    rank = "Смотритель";
                }
                return "Ранг: " + rank;
            case "buy":
                return "Жетоны: " + cost;
            case "level":
                return "Уровень: " + cost;
            default:
                return "Бесплатно";
        }
    }
}
