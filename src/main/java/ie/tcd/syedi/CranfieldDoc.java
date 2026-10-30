package ie.tcd.syedi;

// Represents one document from the Cranfield collection.
public class CranfieldDoc {
    String id;
    StringBuilder title = new StringBuilder();
    StringBuilder author = new StringBuilder();
    StringBuilder source = new StringBuilder();
    StringBuilder body = new StringBuilder();

    public CranfieldDoc(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ID:     " + id + "\n"
             + "TITLE:  " + title.toString().trim() + "\n"
             + "AUTHOR: " + author.toString().trim() + "\n"
             + "SOURCE: " + source.toString().trim() + "\n"
             + "BODY:   " + body.toString().trim() + "\n";
    }
}
