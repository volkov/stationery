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
println "Generate content"
dao.fillContent{ item, store ->
	return [n:0,demand:item.demand]
}

