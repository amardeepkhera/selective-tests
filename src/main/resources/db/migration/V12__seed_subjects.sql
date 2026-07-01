INSERT INTO selective.subject (name)
VALUES ('Reading'),
       ('Mathematical Reasoning'),
       ('Thinking Skills'),
       ('Writing')
ON CONFLICT (name) DO NOTHING;
