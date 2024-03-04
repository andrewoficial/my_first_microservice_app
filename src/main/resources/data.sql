-- INSERT INTO tb_test (created_at, in_archive, name, description, url)
-- VALUES ('01-01-2022', false, 'Натюрморт', 'Описание', 'stilllife');

SELECT * FROM tb_test
WHERE url='stilllife';

-- INSERT INTO tb_picture (created_at, in_archive, description, genre_url, image_path, name)
-- VALUES ('01-01-2023', false, 'Описание...', 'stilllife', 'picture01.png', 'Картина №1');

-- INSERT INTO tb_picture (created_at, in_archive, description, genre_url, image_path, name)
-- VALUES ('01-04-2023', false, 'Описание...', 'stilllife', 'picture02.png', 'Картина №2');

-- INSERT INTO tb_boxes (height, length, width) VALUES (12, 45, 78);
-- INSERT INTO tb_boxes (height, length, width) VALUES (122, 56, 200);
-- INSERT INTO tb_boxes (height, length, width) VALUES (20, 100, 80);
-- INSERT INTO tb_boxes (height, length, width) VALUES (20, 40, 60);


-- schema.sql запросы на создание объектов БД (например, таблиц)
-- data.sql insert, update, delete запросы