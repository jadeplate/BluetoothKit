package benefit.platfrom.bluetooth;

public class MessageEvent {
    private String code;
    private String message;
    private Double message1;
    private Double message2;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getMessage1() {
        return message1;
    }

    public void setMessage1(Double message1) {
        this.message1 = message1;
    }

    public Double getMessage2() {
        return message2;
    }

    public void setMessage2(Double message2) {
        this.message2 = message2;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public MessageEvent(String message){
        this.message=message;
    }
    public MessageEvent(String code , String message){
        this.code = code;
        this.message = message;
    }

    public MessageEvent(String code , Double message1, Double message2, String name){
        this.code = code;
        this.message1 = message1;
        this.message2 = message2;
        this.name = name;
    }

    public MessageEvent(String code , String message, String name){
        this.code = code;
        this.message = message;
        this.name = name;
    }
    public String getMessage() {
        return message;
    }
    public String getCode() {
        return code;
    }


    public void setMessage(String message) {
        this.message = message;
    }
}