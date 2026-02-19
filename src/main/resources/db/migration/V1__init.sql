CREATE TABLE assignments (
  id UUID PRIMARY KEY,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  due_date DATE NOT NULL,
  planning_days INT NOT NULL,
  subject VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL
);

CREATE TABLE availability (
  id UUID PRIMARY KEY,
  mon_hours INT NOT NULL,
  tue_hours INT NOT NULL,
  wed_hours INT NOT NULL,
  thu_hours INT NOT NULL,
  fri_hours INT NOT NULL,
  sat_hours INT NOT NULL,
  sun_hours INT NOT NULL
);

CREATE TABLE tasks (
  id UUID PRIMARY KEY,
  assignment_id UUID NOT NULL,
  effort_hours INT NOT NULL,
  is_done BOOLEAN NOT NULL,
  is_unscheduled BOOLEAN NOT NULL,
  order_index INT NOT NULL,
  target_date DATE,
  title VARCHAR(255) NOT NULL,
  CONSTRAINT fk_tasks_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_assignment_id ON tasks(assignment_id);
