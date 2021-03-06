/*
 * Copyright Bruce Liang (ldcsaa@gmail.com)
 *
 * Version	: JessMA 3.4.1
 * Author	: Bruce Liang
 * Website	: http://www.jessma.org
 * Project	: http://www.oschina.net/p/portal-basic
 * Blog		: http://www.cnblogs.com/ldcsaa
 * WeiBo	: http://weibo.com/u/1402935851
 * QQ Group	: 75375912
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessma.dao.jdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jessma.util.GeneralHelper;

/**
 * 
 * JDBC Session 管理器
 *
 */
public class JdbcSessionMgr extends AbstractJdbcSessionMgr
{
	/** JDBC 默认配置文件 */
	public static final String DEFAULT_CONFIG_FILE	= "jdbc.cfg.xml";
	
	private static final String PROP_DRIVER			= "driver";
	private static final String PROP_URL			= "url";
	
	private Properties info;
	private String url;
	
	@Override
	protected String getDefaultConfigFile()
	{
		return DEFAULT_CONFIG_FILE;
	}
	
	/** 
	 * 初始化 
	 * 
	 * @param args <br>
	 * 			[0]	: configFile （默认：{@link JdbcSessionMgr#DEFAULT_CONFIG_FILE}） <br>
	 * @throws InvalidParameterException
	 * @throws JdbcException
	 * 
	*/
	@Override
	public void initialize(String ... args)
	{
		if(args.length == 0)
			initialize();
		else if(args.length == 1)
			initialize(args[0]);
		else
			throw new InvalidParameterException("JdbcSessionMgr initialize fail (invalid paramers)");
	}
	
	/** 初始化 */
	public void initialize()
	{
		initialize(DEFAULT_CONFIG_FILE);
	}
	
	/** 初始化 */
	public void initialize(String configFile)
	{
		try
		{
			parseConfigFile(configFile);
			
			Properties props = new Properties();
			
			if(isXmlConfigFile())
    			loadXmlCfg(props);
    		else
    			loadPropCfg(props);
			
			info = filtrateCfg(props);

			loadDefalutTransIsoLevel();
		}
		catch(Exception e)
		{
			try {unInitialize();} catch(Exception e2) {}
			throw new JdbcException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadXmlCfg(Properties props) throws Exception
	{
		SAXReader sr	= new SAXReader();
		Document doc	= sr.read(new File(configFile));
		Element root	= doc.getRootElement();
		
		List<Element> list = root.elements("property");
		for(Element e : list)
		{
			String key		= e.attributeValue("name");
			String value	= e.getTextTrim();
			
			props.put(key, value);
		}
	}

	private void loadPropCfg(Properties props) throws IOException
	{
		props.load(new FileInputStream(configFile));
	}

	private Properties filtrateCfg(Properties props)
	{
		Properties result				= new Properties();
		Set<Entry<Object, Object>> set	= props.entrySet();
		
		for(Entry<Object, Object> e : set)
		{
			String key	 = GeneralHelper.safeTrimString((String)e.getKey());
			String value = GeneralHelper.safeTrimString((String)e.getValue());
			if(GeneralHelper.isStrNotEmpty(value))
			{
				if(key.equals(PROP_DRIVER))
					loadDriverClass(value);
				else if(key.equals(PROP_URL))
					url = value;
				else
					result.put(e.getKey(), value);
			}
		}

		return result;
	}

	private void loadDriverClass(String driverClass)
	{
		try
		{
			Class.forName(driverClass);
		}
		catch(ClassNotFoundException e)
		{
			try
			{
				Class.forName(driverClass, true, Thread.currentThread().getContextClassLoader());
			}
			catch(ClassNotFoundException e1)
			{
				throw new JdbcException(e);
			}
		}
	}

	/** 注销 */
	@Override
	public void unInitialize()
	{
		super.unInitialize();
	}
	
	/** 获取数据库连接对象 
	 * @throws SQLException */
	@Override
	protected Connection getInternalConnection() throws SQLException
	{
		return DriverManager.getConnection(url, info);
	}
}
