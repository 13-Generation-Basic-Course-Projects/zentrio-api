package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.model.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DocumentRepository {

    @Select("""
        INSERT INTO documents(created_at,doc_type,user_id,board_id,drive_url,description,folder_id)
        VALUES (#{now}, #{mimeType}, #{uuid}, #{boardId}, #{webViewLink}, #{name}, #{id})
        RETURNING *
        """)
    @Results(id = "folderMapper", value = {
            @Result(property = "documentId", column = "document_id"),
            @Result(property = "creationDate", column = "created_at"),
            @Result(property = "is_public", column = "is_public"),
            @Result(property = "documentType", column = "doc_type"),
            @Result(property = "ownerId", column = "user_id"),
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "drive_url", column = "drive_url"),
            @Result(property = "description", column = "description"),
            @Result(property = "folderId", column = "folder_id")
    })
    Document createFolder(LocalDateTime now, String mimeType, UUID uuid, UUID boardId, String webViewLink, String name, String id);


    @Select("""
            SELECT  * FROM  documents where 
                                   user_id  = #{uuid}     
                                    and board_id= #{boardId}
            """)
    @ResultMap("folderMapper")
    List<Document> getAllFolders(UUID boardId, UUID uuid);


    @Select("""
        SELECT  * FROM  documents 
        WHERE document_id=#{documentId} AND user_id= #{userId}
        """)
    @ResultMap("folderMapper")
    Document getDocumentById(UUID documentId, UUID userId);

    @Select("""
        
        SELECT * FROM  documents
        WHERE folder_id=#{folderId} AND user_id= #{userId}
        """)
    @ResultMap("folderMapper")
    Document getDocumentByFolderId(String folderId, UUID userId);



    @Select("""
        DELETE  FROM documents
        WHERE document_id=#{documentId} AND  user_id=#{userId}
        """)
    void deleteDocumentById(UUID documentId, UUID userId);


        @Select("""
            UPDATE  documents SET description=#{newFolderName}
            WHERE user_id=#{userId} AND  board_id=#{boardId} AND document_id= #{documentId}
            RETURNING *
            """)
        @ResultMap("folderMapper")
        Document updateFolderName(UUID userId, UUID boardId, String newFolderName , UUID documentId);


    @Select(""" 
            UPDATE  documents SET is_public=#{is_private}
            WHERE user_id=#{userID} AND document_id=#{documentId}
            RETURNING *
        """)
    @ResultMap("folderMapper")
    void publicFolder(UUID userID , UUID documentId, boolean is_private);


    @Select("""
        SELECT  * FROM  documents  
        WHERE is_public= true AND board_id= #{boardId}
        """)
    @ResultMap("folderMapper")
    List<Document> getAllPublicDocument(UUID boardId);

    @Select("""
        SELECT * FROM documents 
                 where board_id=#{boardId} AND description ILIKE '%' || #{documentName} || '%' AND user_id=#{userId}
        """)
    @ResultMap("folderMapper")
    List<Document> getDocumentByName(UUID boardId, String documentName, UUID userId);


    @Select("""
        SELECT * FROM documents 
        WHERE user_id=#{uuid} AND board_id=#{boardId} AND doc_type=#{type}
        """)
    @ResultMap("folderMapper")
    List<Document> getDocumentByType(UUID uuid, UUID boardId, String type);

    @Select("""
          SELECT * FROM documents 
                 where document_id=#{documentId}
        """)
    @ResultMap("folderMapper")
    Document getDocumentByDocumentId(UUID documentId);


    @Select("""
            SELECT  * FROM  documents  
            WHERE  board_id= #{boardId}
            """)
    @ResultMap("folderMapper")
    List<Document> getAllDocumentsAllUser(UUID boardId);
}
