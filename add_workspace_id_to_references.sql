-- Add workspace_id column to reference tables for performance optimization
-- This allows efficient filtering by workspace without joining to dict tables

-- 1. Add workspace_id column to ontology_knowlearn_reference
ALTER TABLE ontology_knowlearn_reference 
ADD COLUMN workspace_id BIGINT;

-- 2. Add workspace_id column to ontology_object_reference  
ALTER TABLE ontology_object_reference
ADD COLUMN workspace_id BIGINT;

-- 3. Add workspace_id column to ontology_relation_reference
ALTER TABLE ontology_relation_reference
ADD COLUMN workspace_id BIGINT;

-- 4. Populate workspace_id from related dict tables
-- For knowlearn references
UPDATE ontology_knowlearn_reference ref
SET workspace_id = kt.workspace_id
FROM ontology_knowlearn_type kt
WHERE ref.ontology_knowlearn_id = kt.id;

-- For object references
UPDATE ontology_object_reference ref
SET workspace_id = od.workspace_id
FROM ontology_object_dict od
WHERE ref.ontology_object_id = od.id;

-- For relation references  
UPDATE ontology_relation_reference ref
SET workspace_id = rd.workspace_id
FROM ontology_relation_dict rd
WHERE ref.ontology_relation_id = rd.id;

-- 5. Make workspace_id NOT NULL after populating data
ALTER TABLE ontology_knowlearn_reference
ALTER COLUMN workspace_id SET NOT NULL;

ALTER TABLE ontology_object_reference
ALTER COLUMN workspace_id SET NOT NULL;

ALTER TABLE ontology_relation_reference
ALTER COLUMN workspace_id SET NOT NULL;

-- 6. Create indexes for performance
CREATE INDEX idx_kl_ref_workspace_id ON ontology_knowlearn_reference(workspace_id);
CREATE INDEX idx_obj_ref_workspace_id ON ontology_object_reference(workspace_id);
CREATE INDEX idx_rel_ref_workspace_id ON ontology_relation_reference(workspace_id);

-- 7. Create composite indexes for common query patterns
CREATE INDEX idx_kl_ref_workspace_doc ON ontology_knowlearn_reference(workspace_id, document_id);
CREATE INDEX idx_obj_ref_workspace_doc ON ontology_object_reference(workspace_id, document_id);
CREATE INDEX idx_rel_ref_workspace_doc ON ontology_relation_reference(workspace_id, document_id);
