@Grab(group='com.gmongo', module='gmongo', version='0.9.5')
import com.gmongo.GMongo
import com.mongodb.util.JSON

class DAO {

  def mongo = new GMongo()
  def db = mongo.getDB("stationery")
  def day = 0
  def ext

  DAO(){
    def lastcontent = db.content.find().sort(day:-1)[0]
    if (lastcontent != null){
      day = lastcontent.day
    } else {
			day = 0
    }
  }

  def cleanDb(){
    db.item.drop()
    db.store.drop()
    db.content.drop()
    db.content.ensureIndex(item:1)
    db.content.ensureIndex(store:1)
    db.content.ensureIndex(day:1)
    db.lorder.drop()
    db.delivery.drop()
    db.order.drop()
  }

  def parse(string) {
    return JSON.parse(string)
  }
  
  def eachItemStore(f){
    db.item.find().each{ i ->
      db.store.find(type:"store").each { s ->
        f(i,s)
      }
    }
  }
  
  def eachItemWarehouse(f){
    db.item.find().each{ i ->
      db.store.find(type:"warehouse").each { s ->
        f(i,s)
      }
    }
  }

  
  def fillContent(f){
    db.item.find().each{
      i ->
      db.store.find().each {
        s ->
        def content = f(i,s)
        content.item = i._id
        content.store = s._id
        content.day = 0
        db.content<<content
        //db.content<<[item:i._id,store:s._id,n:1,demand:demand(i,s,0),day:0]
      }
    }
  }

  def iterate(){
    ext.run(db,day)
    db.content.find(day : day).each {
      def item = db.item.findOne(_id : it.item)
      def store = db.store.findOne(_id : it.store)
      db.content <<
      [
        item:it.item,
        store:it.store,
        n:[it.n-it.demand,0].max(),
        demand:ext.demand(item,store,day+1,db),
        day:day+1
      ]
    }
    db.delivery.find(day : day).each {
      delivery ->
      println delivery
      delivery.items.each{
        applyDelivery(delivery,day+1)
      }
    }
    db.order.find(day : day).each {
      order ->
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
