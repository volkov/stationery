def dao = new DAO()
def demand = {item,store,day ->
        if (store.type != 'warehouse') return 1
        return 0
}


dao.fillDb(demand);
dao.iterate(demand);
dao.iterate(demand);
