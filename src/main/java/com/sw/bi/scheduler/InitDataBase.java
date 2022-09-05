package com.sw.bi.scheduler;

import java.io.File;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.NativeQuery;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class InitDataBase {
	static Session session;

    static Configuration config = null;
    static Transaction tx = null;

    public static void main(String[] args) {
        /** */
        /**
         * 根据映射文件创建数据库结构
         */
        try {
            config = new Configuration().configure(new File(
                    "src/hibernate.cfg.xml"));

            System.out.println("Creating tables...");

            SessionFactory sessionFactory = config.buildSessionFactory();
            session = sessionFactory.openSession();
            String hql = "from Action where id = ? ";
            NativeQuery nativeQuery = session.createNativeQuery("select * from user1 ");
            List objects = nativeQuery.list();
            tx = session.beginTransaction();

//            SchemaExport schemaExport = new SchemaExport(config);
//            schemaExport.create(true, true);

            System.out.println("Table created.");
            tx.commit();

        } catch (HibernateException e) {
            e.printStackTrace();
            try {
                tx.rollback();
            } catch (HibernateException e1) {
                e1.printStackTrace();
            }
        } finally {

        }
    }
}
