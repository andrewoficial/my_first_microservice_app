document.addEventListener("DOMContentLoaded", function() {
    const statusCircle = document.getElementById('status-circle');
    const statusText = document.getElementById('status-text');
    const responseTime = document.getElementById('response-time');

    function checkServerStatus() {
        const startTime = Date.now();

        fetch('/api/v1/apps/welcome')
            .then(response => {
                const endTime = Date.now();
                const duration = endTime - startTime;

                responseTime.textContent = `(Задержка: ${duration} мс)`;

                if (duration < 500) {
                    statusCircle.classList.add('green');
                    statusCircle.classList.remove('red');
                    statusText.textContent = "Статус соединения с сервером: хорошо";
                } else {
                    statusCircle.classList.add('red');
                    statusCircle.classList.remove('green');
                    statusText.textContent = "Статус соединения с сервером: плохо";
                }
            })
            .catch(error => {
                statusCircle.classList.add('red');
                statusCircle.classList.remove('green');
                statusText.textContent = "Статус соединения с сервером: ошибка";
                responseTime.textContent = `(Задержка: - мс)`;
            });
    }

    // Проверять статус каждые 5 секунд
    setInterval(checkServerStatus, 5000);
    // Проверить статус сразу при загрузке страницы
    checkServerStatus();
});