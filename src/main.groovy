def dao = new DAO()
dao.ext = new Evg()
(1..365).each { dao.iterate()}

