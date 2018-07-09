import React from "react";
import Field from "./fields/field";
import PropTypes from 'prop-types';
import LeafParam from "./leaf-param";
import {Panel} from "react-bootstrap";
import RemoveButton from "./fields/remove-button";
import AddButton from "./fields/add-button";

/**
 * Param with child params
 */
export default class Param extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            opened: true
        };
    }

    change = (param) => {
        this.props.onChange(param);
    };

    changeChild = (index) => (child) => {
        let param = this.props.param;
        param.params[index] = child;
        this.change(param);
    };

    add = () => () => {
        this.props.onAdd();
    };

    remove = () => () => {
        this.props.onRemove();
    };

    handlePanelClick = () => {
        this.setState({opened: !this.state.opened});
    };

    renderParentField(param) {
        return <div className="parent-param-field">
            <Field
                index={0}
                param={param}
                onChange={this.change}
                onAdd={this.add}
                onRemove={this.remove}
                bare={true}
            />
        </div>;
    }

    renderChildParams(param) {
        return param.params.map((childParam, i) => {
            return <LeafParam
                key={i}
                param={childParam}
                onAdd={this.add}
                onRemove={this.remove}
                onChange={this.changeChild()}
            />;
        }, this);
    }

    render() {
        let param = this.props.param;
        let hasChildren = Array.isArray(param.params);
        if (hasChildren) {
            return (
                <div>
                    <Panel>
                        <Panel.Heading>
                            <Panel.Title>
                                <RemoveButton
                                    canRemove={this.props.param.canRemove}
                                    onRemove={this.remove}
                                />
                                <AddButton
                                    canAdd={this.props.param.canAdd}
                                    onAdd={this.add}
                                />
                                <div className="clickable"
                                     onClick={this.handlePanelClick}
                                >
                                    {param.label ? param.label : param.name}
                                </div>
                            </Panel.Title>
                        </Panel.Heading>
                        <Panel.Body className={this.state.opened ? '' : 'collapse'}>
                            {this.renderParentField(param)}
                            {this.renderChildParams(param)}
                        </Panel.Body>
                    </Panel>
                </div>
            );
        } else {
            return (
                <LeafParam
                    param={param}
                    onAdd={this.add}
                    onRemove={this.remove}
                    onChange={this.change}
                />
            );
        }
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired
};