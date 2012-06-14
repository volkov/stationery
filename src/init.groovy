def dao = new DAO()

println "Drop collections..."
dao.cleanDb()

println "Fill items from ${args[0]}"
def items = new File(args[0])
items.eachLine {
	item = dao.parse(it)
	if (item.profit==null) item.profit=item.price*0.3d
	dao.db.item<<item
}
println "Fill stores from ${args[1]}"
def stores = new File(args[1])
stores.eachLine {
	store = dao.parse(it)
	dao.db.store<<store
}
println "Generate past demand from ${args[2]}"
def demand = [] 
new File(args[2]).eachLine{ demand << (Float.parseFloat(it)*Float.parseFloat(it)*Float.parseFloat(it))}
println demand
dao.eachItemStore{ i,s->
  ((1-demand.size())..0).each{
    d = (Integer) (demand[-it]*i.demand).round()
    dao.db.content<< [item:i._id,store:s._id,demand:(Integer)(d*s.demand).round(),n:d,day:it]
  }
}
dao.eachItemWarehouse{ i,s->
  ((1-demand.size())..0).each{
    dao.db.content << [item:i._id,store:s._id,demand:0,n:i.demand*30*3,day:it]
  }
}

