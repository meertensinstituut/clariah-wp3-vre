package nl.knaw.meertens.clariah.vre.tagger;

public class NameValueDto {
    public String name;
    public Object value;

    public <T> NameValueDto(String name, T value) {
        this.name = name;
        this.value = value;
    }
}
