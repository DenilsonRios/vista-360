insert into program (id, name, faculty) values
    (1, 'Ingeniería de Sistemas', 'Facultad de Ingeniería'),
    (2, 'Administración de Empresas', 'Facultad de Ciencias Administrativas');

insert into academic_term (id, code, name, start_date, end_date, is_current) values
    (1, '2025-1', 'Primer semestre 2025', DATE '2025-01-20', DATE '2025-05-30', false),
    (2, '2025-2', 'Segundo semestre 2025', DATE '2025-07-21', DATE '2025-11-28', true);

insert into student (id, code, document, first_name, last_name, email, status, program_id) values
    (1, 'A00123456', '1144012345', 'Laura', 'Gómez',    'laura.gomez@u.edu.co',      'ACTIVE', 1),
    (2, 'A00987654', '1144098765', 'Carlos', 'Ramírez', 'carlos.ramirez@u.edu.co',   'ACTIVE', 2),
    (3, 'A00111222', '1144011122', 'Ana',   'Peña',     'ana.pena@u.edu.co',         'ACTIVE', 1);

insert into course (id, code, name, credits) values
    (1, 'IS-101',  'Introducción a la Ingeniería de Software', 3),
    (2, 'IS-205',  'Estructuras de Datos',                     4),
    (3, 'MAT-201', 'Cálculo Diferencial',                      4),
    (4, 'ADM-110', 'Fundamentos de Administración',            3);

insert into course_offering (id, course_id, term_id, group_code, professor_name) values
    (1, 1, 2, '01', 'Ana Torres'),
    (2, 2, 2, '01', 'Pedro Salas'),
    (3, 3, 2, '02', 'Marta Ríos'),
    (4, 4, 2, '01', 'Jorge León'),
    (5, 1, 1, '01', 'Ana Torres');

insert into enrollment (id, student_id, course_offering_id, status, enrolled_at, grade) values
    (1, 1, 1, 'ENROLLED',  TIMESTAMP '2025-07-15 09:00:00', 4.30),
    (2, 1, 2, 'ENROLLED',  TIMESTAMP '2025-07-15 09:05:00', null),
    (3, 1, 3, 'ENROLLED',  TIMESTAMP '2025-07-15 09:10:00', 3.80),
    (4, 1, 5, 'COMPLETED', TIMESTAMP '2025-01-22 08:00:00', 4.50),
    (5, 2, 4, 'ENROLLED',  TIMESTAMP '2025-07-16 10:00:00', 4.00),
    (6, 3, 1, 'WITHDRAWN', TIMESTAMP '2025-07-15 11:00:00', null);

insert into advisor_assignment (id, advisor_subject, student_id, active) values
    (1, 'advisor-001', 1, true),
    (2, 'advisor-001', 3, true);
