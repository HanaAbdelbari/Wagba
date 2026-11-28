-- Names must be unique among ACTIVE option groups of an item and ACTIVE options of a group.
-- Archived rows are ignored, so an archived name can be reused.
CREATE UNIQUE INDEX ux_option_groups_active_name ON option_groups (item_id, lower(name)) WHERE is_active;
CREATE UNIQUE INDEX ux_options_active_name       ON options (group_id, lower(name))       WHERE is_active;