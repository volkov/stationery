outfolder=new File(args[0])

dao = new DAO()
dao.db.store.find().each { s ->
  dao.db.item.find().each { i ->
    out = new File(outfolder,"${s.name}-${i.item}")
    dao.db.content.find(store:s._id,item:i._id).sort(day:1).each{
      out<<"${it.day} ${it.n} ${it.demand} \n"
    }
  }
}