@Grab(group='com.gmongo', module='gmongo', version='0.9.5')
import com.gmongo.GMongo
import com.mongodb.util.JSON

class DAO {

	def mongo = new GMongo()
	def db = mongo.getDB("stationery")
	def day = 0	
	
	def cleanDb(){
		db.item.drop()
		db.store.drop()
		db.content.drop()
		db.delivery.drop()
		db.order.drop()
	}

	def parse(string) {
		return JSON.parse(string)
	}
	
	def fillDb(demand) {
		db.item.drop()
		db.item << [name:'Pen',profit:1.4d,v:0.9d]
		def pen = db.item.findOne(name:'Pen')
		db.store.drop()
		db.store << [name:'Lenta',type:'store']
		def lenta = db.store.findOne(name : 'Lenta')
		db.store << [name:'Warehouse',type:'warehouse']
		def warehouse = db.store.findOne(name : 'Warehouse')
		db.content.drop()
		db.item.find().each{ i ->
			db.store.find().each { s ->
				db.content<<[item:i._id,store:s._id,n:1,demand:demand(i,s,0),day:0]
			}
		}
		db.delivery.drop()
		db.delivery << [
			items:[[item:pen._id,n:1]],
			from:warehouse._id,
			to:lenta._id,
			day:0
		]
		db.order.drop()
		db.order << [
			items:[[item:pen._id,n:1]],
			to:warehouse._id,
			day:0
		]
	}

	def iterate(demand){
		db.content.find(day : day).each {
			def item = db.item.findOne(_id : it.item)
			def store = db.store.findOne(_id : it.store)
			db.content << 
			[
				item:it.item,
				store:it.store,
				n:[it.n-it.demand,0].max(),
				demand:demand(item,store,day+1),
				day:day+1
			]
		}
		db.delivery.find(day : day).each { delivery ->
			println delivery
			delivery.items.each{
				applyDelivery(delivery,day+1)
			}
		}
		db.order.find(day : day-1).each { order ->
			println order
			order.items.each{
				applyDelivery(order,day+1)
			}
		}
		
		day++
	}
	
	def applyDelivery(delivery,day){
		delivery.items.each{
			def from = db.content.findOne(store:delivery.from,item:it.item,day:day)
			def res
			if (from!=null) { 
				res = [it.n,from.n].min() 
				db.content.update([_id:from._id],[$set:[n:from.n-res]])
			} else {
				res = it.n
			}
			def to = db.content.findOne(store:delivery.to,item:it.item,day : day)
			db.content.update([_id:to._id],[$set:[n:to.n+res]])
		}
	}
}
