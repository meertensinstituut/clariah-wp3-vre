import React from "react";
import Switchboard from "../common/switchboard";
import Field from "./form/field";

export default class Configurator extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            form: {
                params: [

                ]
            },
            serviceParams: null
        };
        if (this.props.service !== undefined) {
            this.getServiceParams(this.props.service);
        }
        this.onChangeParam = this.onChangeParam.bind(this);
    }

    onChangeParam(formParam, newFormParam) {

    }

    getServiceParams(service) {
        Switchboard.getParams(service).then((data) => {
            this.setState({
                serviceParams: data
            });
        });
    }

    render() {
        const serviceParams = this.state.serviceParams;

        if (serviceParams === null) return <div>Loading...</div>;

        return (
            <div>
                <form>
                    {serviceParams.params.map((param, i) => {
                        let formParam = {};
                        this.state.form.params.push(formParam);
                        return <Field key={i} param={param} value={formParam} onChange={this.onChangeParam(formParam, newFormParam)} />
                    }, this)}
                    {serviceParams.paramGroups.map((group, i) => {
                        return <div key={i}>
                            <Field key={i} param={group} />
                            {group.params.map((param, k) => {
                                return <Field key={k} param={param} />
                            }, this)}
                        </div>
                    }, this)}
                </form>
                <div>
                    <p>state:</p>
                    <pre>{JSON.stringify(this.state, null, 2)}</pre>
                </div>
                <div>
                    <p>props:</p>
                    <pre>{JSON.stringify(this.props, null, 2)}</pre>
                </div>
            </div>
        );
    }
}