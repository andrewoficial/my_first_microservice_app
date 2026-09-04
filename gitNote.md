# GitFlic push conflict

Если при пуше на GitFlic ошибка:

```
! [rejected] (non-fast-forward)
hint: fetch first / pull
```

Значит удалённая ветка на GitFlic ушла вперёд (например, после пуша через веб-интерфейс).

## Исправление

```bash
# спрятать незакоммиченные изменения (если есть)
git stash

# стянуть с GitFlic и перебазировать свой коммит поверх
git pull --rebase gitflic master

# достать изменения обратно
git stash pop

# запушить на GitFlic
git push gitflic master

# на GitHub — форс-пуш (хеш коммита изменился после rebase)
git push --force-with-lease github master
```
