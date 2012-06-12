def dao = new DAO()
dao.ext = new Evg()
(1..100).each { dao.iterate()}

