package nl.knaw.meertens.clariah.vre.indexer.dto;

public class SolrRecordDTO {

    public String object_id = null;
    public String metadata_id = null;
    public String filepath = null;
    public Integer filesize = null;
    public String user_id = null;
    public String created = null;
    public String changed = null;
    public String format = null;
    public String mimetype = null;
    public String mtas_text = null;
    public String mtas_type = null;
    
    public SolrRecordDTO(ObjectsRecordDTO record) {
      final String MIMETYPE_XML = "text/xml";      
      final String TYPE_FOLIA = "folia";      
      object_id = record.id;
      metadata_id = record.metadata_id;
      filepath = record.filepath;
      filesize = record.filesize;
      user_id = record.user_id;
      created = record.time_created;
      changed = record.time_changed;
      format = record.format;
      mimetype = record.mimetype; 
      if(mimetype.equals(MIMETYPE_XML)) {
        mtas_text = filepath;
        mtas_type = TYPE_FOLIA;
      }
    }
    
}
